package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The operator-tunable half of the gate. See ADR-0002 for why this is split from CandidateProfile. */
@Validated
@ConfigurationProperties(prefix = "joblens.gate")
public record GateProperties(@DecimalMin("0.0") double maxRedFlagScore) {
}
