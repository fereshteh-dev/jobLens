package io.github.fereshtehdev.joblens.analysis.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full context, real config binding, real mock adapters (no @MockitoBean) - proves the whole
 * Phase 2 assembly actually works, independent of any local candidate-profile.yml.
 */
@SpringBootTest(properties = {
        "joblens.gate.max-red-flag-score=1.5",
        "joblens.candidate-profile.target-seniority=SENIOR",
        "joblens.candidate-profile.minimum-sponsorship-likelihood=UNKNOWN"
})
@AutoConfigureMockMvc
class AnalyzeEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void matchingSeniorPosting_passesGateAndReturnsTalkingPoints() throws Exception {
        String requestJson = """
                {"url": "https://example.com/job", "title": "Senior Backend Engineer", "company": "Acme",
                 "location": "Remote", "descriptionText": "Competitive salary, $150k-$180k, great team."}
                """;

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seniority.claimed").value("SENIOR"))
                .andExpect(jsonPath("$.seniority.actual").value("SENIOR"))
                .andExpect(jsonPath("$.gate.passed").value(true))
                .andExpect(jsonPath("$.talkingPoints").exists());
    }

    @Test
    void mismatchedSeniorityPosting_failsGateAndOmitsTalkingPoints() throws Exception {
        String requestJson = """
                {"url": "https://example.com/job", "title": "Junior Backend Engineer", "company": "Acme",
                 "location": "Remote", "descriptionText": "Entry level role, salary $60k."}
                """;

        mockMvc.perform(post("/api/v1/analyze").contentType("application/json").content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seniority.actual").value("JUNIOR"))
                .andExpect(jsonPath("$.gate.passed").value(false))
                .andExpect(jsonPath("$.gate.reasons[0]").value(org.hamcrest.Matchers.containsString("seniority")))
                .andExpect(jsonPath("$.talkingPoints").doesNotExist());
    }
}
