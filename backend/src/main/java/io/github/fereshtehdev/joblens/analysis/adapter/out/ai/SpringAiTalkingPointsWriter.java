package io.github.fereshtehdev.joblens.analysis.adapter.out.ai;

import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsUnavailableException;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsWriter;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.config.properties.TalkingPointsResilienceProperties;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Real talking-points adapter (Spring AI ChatClient). Only active when {@code joblens.ai.enabled=true}
 * (default false - see StubTalkingPointsWriter, the offline default). Never called for
 * classification - see ADR-0003. Retry + circuit breaker are composed by hand with
 * Resilience4j's core modules (not the Spring Boot integration - see ADR-0005): it refuses to
 * start on anything but Spring Boot 3.x, confirmed by actually running this app on Boot 4.1.1.
 */
@Component
@ConditionalOnProperty(prefix = "joblens.ai", name = "enabled", havingValue = "true")
public class SpringAiTalkingPointsWriter implements TalkingPointsWriter {

    private static final Logger log = LoggerFactory.getLogger(SpringAiTalkingPointsWriter.class);
    private static final String RESILIENCE_INSTANCE = "talkingPoints";

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public SpringAiTalkingPointsWriter(ChatClient.Builder chatClientBuilder, MeterRegistry meterRegistry,
                                        TalkingPointsResilienceProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.meterRegistry = meterRegistry;
        this.retry = Retry.of(RESILIENCE_INSTANCE, RetryConfig.custom()
                .maxAttempts(properties.retryMaxAttempts())
                .waitDuration(properties.retryWaitDuration())
                .build());
        this.circuitBreaker = CircuitBreaker.of(RESILIENCE_INSTANCE, CircuitBreakerConfig.custom()
                .slidingWindowSize(properties.circuitBreakerSlidingWindowSize())
                .minimumNumberOfCalls(properties.circuitBreakerMinimumCalls())
                .failureRateThreshold(properties.circuitBreakerFailureRateThreshold())
                .waitDurationInOpenState(properties.circuitBreakerWaitDurationInOpenState())
                .build());
    }

    @Override
    public TalkingPoints write(JobPosting posting, Analysis analysis, CandidateProfile profile) {
        Supplier<TalkingPoints> call = () -> callModel(posting, analysis, profile);
        Supplier<TalkingPoints> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call));
        try {
            return decorated.get();
        } catch (Exception e) {
            throw translateFailure(e);
        }
    }

    private TalkingPoints callModel(JobPosting posting, Analysis analysis, CandidateProfile profile) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return chatClient.prompt()
                    .system(TalkingPointsPrompt.SYSTEM)
                    .user(TalkingPointsPrompt.user(posting, analysis, profile))
                    .call()
                    .entity(TalkingPoints.class);
        } finally {
            sample.stop(meterRegistry.timer("joblens.talking-points.latency"));
        }
    }

    // Package-private so it's directly unit-testable without exercising the ChatClient chain.
    TalkingPointsUnavailableException translateFailure(Throwable throwable) {
        log.warn("Talking points generation failed ({}: {}); returning analysis without talking points",
                throwable.getClass().getSimpleName(), throwable.getMessage());
        return new TalkingPointsUnavailableException("Talking points generation failed", throwable);
    }
}
