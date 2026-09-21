package io.github.fereshtehdev.joblens.analysis.adapter.in.web;

import io.github.fereshtehdev.joblens.analysis.application.AnalysisOutcome;
import io.github.fereshtehdev.joblens.analysis.application.AnalyzePostingUseCase;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.GateDecision;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The candidate profile is mocked below, but {@code @EnableConfigurationProperties} on
 * {@code JoblensApplication} still eagerly binds+validates {@code CandidateProfileProperties}
 * even in this slice, so it needs valid values regardless of whether this test uses them.
 */
@WebMvcTest(AnalyzeController.class)
@TestPropertySource(properties = {
        "joblens.candidate-profile.target-seniority=SENIOR",
        "joblens.candidate-profile.minimum-sponsorship-likelihood=UNKNOWN"
})
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyzePostingUseCase useCase;

    @MockitoBean
    private CandidateProfile candidateProfile;

    @Test
    void gatePassed_returnsTalkingPoints() throws Exception {
        when(useCase.analyze(any(), any())).thenReturn(new AnalysisOutcome.WithTalkingPoints(
                matchingAnalysis(), GateDecision.passing(),
                new TalkingPoints(List.of("angle"), List.of("question"), List.of("fit"))));

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gate.passed").value(true))
                .andExpect(jsonPath("$.talkingPoints.coverLetterAngles[0]").value("angle"))
                .andExpect(jsonPath("$.seniority.actual").value("SENIOR"))
                .andExpect(jsonPath("$.seniority.titleVsReality.kind").value("CONFIDENT"));
    }

    @Test
    void gateFailed_omitsTalkingPoints() throws Exception {
        when(useCase.analyze(any(), any())).thenReturn(new AnalysisOutcome.WithoutTalkingPoints(
                matchingAnalysis(), GateDecision.failed(List.of("Actual seniority MID does not match target SENIOR"))));

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gate.passed").value(false))
                .andExpect(jsonPath("$.gate.reasons[0]").exists())
                .andExpect(jsonPath("$.talkingPoints").doesNotExist());
    }

    @Test
    void blankRequiredField_returns400ProblemDetail() throws Exception {
        String invalidJson = """
                {"url": "", "title": "Engineer", "company": "Acme", "location": "Remote", "descriptionText": "Great role."}
                """;

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void useCaseThrowsUnexpectedException_returns500WithoutStackTrace() throws Exception {
        when(useCase.analyze(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(validRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Unexpected error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."));
    }

    private static String validRequestJson() {
        return """
                {"url": "https://example.com/job", "title": "Senior Engineer", "company": "Acme",
                 "location": "Remote", "descriptionText": "Great role with a competitive salary."}
                """;
    }

    private static Analysis matchingAnalysis() {
        return new Analysis(
                new SeniorityAssessment(Seniority.SENIOR, Seniority.SENIOR, new Verdict.Confident(new Probability(0.9))),
                List.of(new RedFlag(RedFlagKind.HIDDEN_OVERTIME, new Verdict.Ignored())),
                new SponsorshipAssessment(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY),
                "mock-classifier-0");
    }
}
