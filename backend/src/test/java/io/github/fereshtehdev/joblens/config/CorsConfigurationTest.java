package io.github.fereshtehdev.joblens.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies WebConfig's CORS registration actually restricts to the configured origin. */
@SpringBootTest(properties = {
        "joblens.gate.max-red-flag-score=1.5",
        "joblens.candidate-profile.target-seniority=SENIOR",
        "joblens.candidate-profile.minimum-sponsorship-likelihood=UNKNOWN",
        "joblens.security.allowed-origins=chrome-extension://allowed-id"
})
@AutoConfigureMockMvc
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowedOrigin_getsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/analyze")
                        .header("Origin", "chrome-extension://allowed-id")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "chrome-extension://allowed-id"));
    }

    @Test
    void disallowedOrigin_getsNoCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/analyze")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
