package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import io.github.fereshtehdev.joblens.analysis.application.port.ClassificationUnavailableException;
import io.github.fereshtehdev.joblens.analysis.application.port.PostingClassifier;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.ClaimedSeniorityParser;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.ThresholdPolicy;
import io.github.fereshtehdev.joblens.analysis.domain.ThresholdSettings;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import io.github.fereshtehdev.joblens.config.properties.JevResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The real classifier - Jev does all classification/scoring, per ADR-0003. Only active when
 * {@code joblens.jev.enabled=true} (default false; MockPostingClassifier is the offline
 * default). A failure here is deliberately never caught by AnalyzePostingUseCase: it
 * propagates as {@link ClassificationUnavailableException}, mapped to a 502 by
 * ProblemDetailHandler - "if Jev is down, return a clear error" (CLAUDE.md), never a silent
 * fallback.
 */
@Component
@ConditionalOnProperty(prefix = "joblens.jev", name = "enabled", havingValue = "true")
public class JevPostingClassifier implements PostingClassifier {

    private static final Logger log = LoggerFactory.getLogger(JevPostingClassifier.class);
    private static final String RESILIENCE_INSTANCE = "jev";

    private final JevHttpClient httpClient;
    private final JevQuestionSchema schema;
    private final ThresholdSettings thresholdSettings;
    private final MeterRegistry meterRegistry;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    JevPostingClassifier(JevHttpClient httpClient, JevQuestionSchema schema, ThresholdSettings thresholdSettings,
                          MeterRegistry meterRegistry, JevResilienceProperties resilienceProperties) {
        this.httpClient = httpClient;
        this.schema = schema;
        this.thresholdSettings = thresholdSettings;
        this.meterRegistry = meterRegistry;
        this.retry = Retry.of(RESILIENCE_INSTANCE, RetryConfig.custom()
                .maxAttempts(resilienceProperties.retryMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        resilienceProperties.retryInitialInterval(), resilienceProperties.retryBackoffMultiplier()))
                .retryOnException(JevPostingClassifier::isTransient)
                .build());
        this.circuitBreaker = CircuitBreaker.of(RESILIENCE_INSTANCE, CircuitBreakerConfig.custom()
                .slidingWindowSize(resilienceProperties.circuitBreakerSlidingWindowSize())
                .minimumNumberOfCalls(resilienceProperties.circuitBreakerMinimumCalls())
                .failureRateThreshold(resilienceProperties.circuitBreakerFailureRateThreshold())
                .waitDurationInOpenState(resilienceProperties.circuitBreakerWaitDurationInOpenState())
                .build());
    }

    @Override
    public Analysis classify(JobPosting posting) {
        Supplier<JevResponse> call = () -> callJev(posting);
        Supplier<JevResponse> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call));

        JevResponse response;
        try {
            response = decorated.get();
        } catch (Exception e) {
            log.warn("Jev classification failed ({}: {})", e.getClass().getSimpleName(), e.getMessage());
            throw new ClassificationUnavailableException("Jev classification failed", e);
        }

        log.info("Jev classification completed using model {}", response.model());
        try {
            return toAnalysis(posting, response);
        } catch (RuntimeException e) {
            throw new ClassificationUnavailableException("Jev response could not be interpreted", e);
        }
    }

    private JevResponse callJev(JobPosting posting) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return httpClient.classify(buildState(posting), buildQuestions());
        } finally {
            sample.stop(meterRegistry.timer("joblens.jev.latency"));
        }
    }

    private static String buildState(JobPosting posting) {
        // title_vs_reality needs the title as context - it isn't part of descriptionText.
        return """
                Title: %s
                Company: %s
                Location: %s

                %s""".formatted(posting.title(), posting.company(), posting.location(), posting.descriptionText());
    }

    private Map<String, Object> buildQuestions() {
        Map<String, Object> questions = new LinkedHashMap<>();
        schema.questions().forEach((id, definition) -> {
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("type", definition.type());
            question.put("instructions", definition.instructions());
            if (!definition.criteria().isEmpty()) {
                question.put("criteria", definition.criteria());
            }
            questions.put(id, question);
        });
        return questions;
    }

    private Analysis toAnalysis(JobPosting posting, JevResponse response) {
        Seniority claimed = ClaimedSeniorityParser.parse(posting.title());
        Seniority actual = parseSeniority(answer(response, "actual_seniority").choice());
        Verdict titleVsRealityVerdict = verdictFrom(answer(response, "title_vs_reality").confidence());
        SeniorityAssessment seniority = new SeniorityAssessment(claimed, actual, titleVsRealityVerdict);

        List<RedFlag> redFlags = Arrays.stream(RedFlagKind.values())
                .map(kind -> new RedFlag(kind, verdictFrom(answer(response, jevKey(kind)).noul())))
                .toList();

        SponsorshipAssessment sponsorship = new SponsorshipAssessment(
                parseSponsorship(answer(response, "visa_sponsorship").choice()),
                parseSponsorship(answer(response, "relocation_support").choice()));

        return new Analysis(seniority, redFlags, sponsorship, response.model());
    }

    private static JevResponse.JevAnswer answer(JevResponse response, String key) {
        JevResponse.JevAnswer answer = response.answers().get(key);
        if (answer == null) {
            throw new IllegalStateException("Jev response is missing answer for question '" + key + "'");
        }
        return answer;
    }

    private static String jevKey(RedFlagKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private Verdict verdictFrom(Double probability) {
        if (probability == null) {
            throw new IllegalStateException("Jev answer is missing a probability/confidence value");
        }
        return ThresholdPolicy.toVerdict(new Probability(probability), thresholdSettings);
    }

    private static Seniority parseSeniority(String choice) {
        return Seniority.valueOf(requireChoice(choice).toUpperCase(Locale.ROOT));
    }

    private static SponsorshipLikelihood parseSponsorship(String choice) {
        return SponsorshipLikelihood.valueOf(requireChoice(choice).toUpperCase(Locale.ROOT));
    }

    private static String requireChoice(String choice) {
        if (choice == null || choice.isBlank()) {
            throw new IllegalStateException("Jev answer is missing a choice value");
        }
        return choice;
    }

    // Retry only what the brief calls "idempotent transient failures": network/timeout, 5xx
    // (including Jev's 529 overloaded), and 429 rate limiting. Never 401/422 - retrying a bad
    // key or a malformed request just wastes attempts.
    private static boolean isTransient(Throwable throwable) {
        if (throwable instanceof ResourceAccessException) {
            return true;
        }
        if (throwable instanceof HttpServerErrorException) {
            return true;
        }
        return throwable instanceof HttpClientErrorException httpClientError
                && httpClientError.getStatusCode().value() == 429;
    }
}
