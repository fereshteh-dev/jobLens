package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The "high"/"middle" cutoffs from the brief - see {@code ThresholdSettings}. */
@Validated
@ConfigurationProperties(prefix = "joblens.thresholds")
public record ThresholdProperties(@NotNull Double confidentAt, @NotNull Double uncertainAt) {
}
