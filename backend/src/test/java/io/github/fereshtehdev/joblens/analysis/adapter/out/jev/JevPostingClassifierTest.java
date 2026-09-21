package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import io.github.fereshtehdev.joblens.analysis.application.port.ClassificationUnavailableException;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.ThresholdSettings;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import io.github.fereshtehdev.joblens.config.properties.JevProperties;
import io.github.fereshtehdev.joblens.config.properties.JevResilienceProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class JevPostingClassifierTest {

    private static final String BASE_URL = "https://jev.test/v1";
    private static final JobPosting POSTING = new JobPosting(
            "https://example.com/job", "Senior Backend Engineer", "Acme", "Remote",
            "Own our payments platform end to end. Salary $150k-$180k.");

    private static final String SUCCESS_RESPONSE = """
            {
              "model": "jev-1.13.0",
              "answers": {
                "actual_seniority": {"type": "choice", "choice": "senior", "confidence": 0.91,
                  "probabilities": {"junior": 0.0, "mid": 0.05, "senior": 0.91, "staff_plus": 0.04}},
                "title_vs_reality": {"type": "choice", "choice": "match", "confidence": 0.85,
                  "probabilities": {"match": 0.85, "inflated_title": 0.1, "understated_title": 0.05}},
                "hidden_overtime": {"type": "noul", "noul": 0.1},
                "vague_scope": {"type": "noul", "noul": 0.05},
                "too_many_roles": {"type": "noul", "noul": 0.02},
                "no_compensation_info": {"type": "noul", "noul": 0.0},
                "unrealistic_requirements": {"type": "noul", "noul": 0.15},
                "high_turnover_signals": {"type": "noul", "noul": 0.9},
                "visa_sponsorship": {"type": "choice", "choice": "unlikely", "confidence": 0.7,
                  "probabilities": {"explicit_yes": 0.05, "likely": 0.1, "unlikely": 0.7, "explicit_no": 0.05, "unknown": 0.1}},
                "relocation_support": {"type": "choice", "choice": "unknown", "confidence": 0.4,
                  "probabilities": {"explicit_yes": 0.1, "likely": 0.1, "unlikely": 0.2, "explicit_no": 0.1, "unknown": 0.5}}
              },
              "usage": {"input_tokens": 512, "output_tokens": 120}
            }""";

    @Test
    void classify_sendsExpectedRequestAndMapsFullResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/systemone"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("jev-latest"))
                .andExpect(jsonPath("$.state").value(org.hamcrest.Matchers.containsString("Senior Backend Engineer")))
                .andExpect(jsonPath("$.questions.actual_seniority.type").value("choice"))
                .andExpect(jsonPath("$.questions.actual_seniority.criteria.senior").exists())
                .andExpect(jsonPath("$.questions.hidden_overtime.type").value("noul"))
                .andExpect(jsonPath("$.questions.hidden_overtime.criteria").doesNotExist())
                .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        JevPostingClassifier classifier = classifier(builder, minimalSchema());

        Analysis analysis = classifier.classify(POSTING);

        assertThat(analysis.jevModelVersion()).isEqualTo("jev-1.13.0");
        assertThat(analysis.seniority().claimed()).isEqualTo(Seniority.SENIOR);
        assertThat(analysis.seniority().actual()).isEqualTo(Seniority.SENIOR);
        assertThat(analysis.seniority().titleVsRealityVerdict()).isEqualTo(new Verdict.Confident(new Probability(0.85)));
        assertThat(analysis.sponsorship().visa()).isEqualTo(SponsorshipLikelihood.UNLIKELY);
        assertThat(analysis.sponsorship().relocation()).isEqualTo(SponsorshipLikelihood.UNKNOWN);

        assertThat(redFlagVerdict(analysis, RedFlagKind.HIGH_TURNOVER_SIGNALS)).isEqualTo(new Verdict.Confident(new Probability(0.9)));
        assertThat(redFlagVerdict(analysis, RedFlagKind.HIDDEN_OVERTIME)).isEqualTo(new Verdict.Ignored());

        server.verify();
    }

    @Test
    void classify_afterRetriesExhausted_throwsClassificationUnavailableException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(BASE_URL + "/systemone")).andRespond(withServerError());
        }

        JevPostingClassifier classifier = classifier(builder, minimalSchema());

        assertThatThrownBy(() -> classifier.classify(POSTING))
                .isInstanceOf(ClassificationUnavailableException.class)
                .hasCauseInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void classify_transientFailureThenSuccess_recoversViaRetry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/systemone")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/systemone")).andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON));

        JevPostingClassifier classifier = classifier(builder, minimalSchema());

        Analysis analysis = classifier.classify(POSTING);

        assertThat(analysis.jevModelVersion()).isEqualTo("jev-1.13.0");
        server.verify();
    }

    private static JevPostingClassifier classifier(RestClient.Builder builder, JevQuestionSchema schema) {
        JevProperties properties = new JevProperties(BASE_URL, "test-key", "jev-latest");
        JevHttpClient httpClient = new JevHttpClient(builder, properties);
        ThresholdSettings thresholdSettings = new ThresholdSettings(new Probability(0.8), new Probability(0.4));
        JevResilienceProperties resilienceProperties = new JevResilienceProperties(
                3, Duration.ofMillis(5), 1.0, 10, 5, 50f, Duration.ofSeconds(1));
        return new JevPostingClassifier(httpClient, schema, thresholdSettings, new SimpleMeterRegistry(), resilienceProperties);
    }

    private static JevQuestionSchema minimalSchema() {
        return new JevQuestionSchema(Map.of(
                "actual_seniority", new JevQuestionSchema.JevQuestionDefinition("choice", "Classify seniority.",
                        Map.of("junior", "j", "mid", "m", "senior", "s", "staff_plus", "sp")),
                "hidden_overtime", new JevQuestionSchema.JevQuestionDefinition("noul", "Hidden overtime?", null)));
    }

    private static Verdict redFlagVerdict(Analysis analysis, RedFlagKind kind) {
        return analysis.redFlags().stream()
                .filter(flag -> flag.kind() == kind)
                .findFirst()
                .orElseThrow()
                .verdict();
    }
}
