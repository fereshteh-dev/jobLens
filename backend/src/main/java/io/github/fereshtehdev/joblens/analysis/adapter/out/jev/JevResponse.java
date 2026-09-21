package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Response body from {@code POST /v1/systemone}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record JevResponse(String model, Map<String, JevAnswer> answers, JevUsage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JevUsage(@JsonProperty("input_tokens") int inputTokens, @JsonProperty("output_tokens") int outputTokens) {
    }

    /**
     * One flattened shape for all three answer types (choice/score/noul), rather than
     * polymorphic Jackson types - simpler, and this app only ever reads {@code choice} or
     * {@code noul}/{@code confidence} depending on the question, never {@code score}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JevAnswer(String type, String choice, Double confidence, Map<String, Double> probabilities, Double noul,
                      Double score, Map<String, String> legend) {
    }
}
