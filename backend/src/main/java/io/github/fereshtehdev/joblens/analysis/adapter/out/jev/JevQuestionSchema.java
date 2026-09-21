package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * The Jev question set - labels, instructions, criteria - loaded as data from
 * {@code jev-questions.yml}, not scattered through Java string literals (see the brief and
 * ADR-0002). Question ids are expected to match {@code RedFlagKind} names (lowercased) for
 * the six red-flag questions, plus {@code actual_seniority}, {@code title_vs_reality},
 * {@code visa_sponsorship} and {@code relocation_support}.
 */
@Validated
@ConfigurationProperties(prefix = "jev-schema")
public record JevQuestionSchema(Map<String, JevQuestionDefinition> questions) {

    public record JevQuestionDefinition(String type, String instructions, Map<String, String> criteria) {

        public JevQuestionDefinition {
            if (criteria == null) {
                criteria = Map.of();
            }
        }
    }
}
