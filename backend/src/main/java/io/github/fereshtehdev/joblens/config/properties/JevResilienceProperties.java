package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Retry/circuit-breaker tuning for {@code JevPostingClassifier}. Retry uses exponential
 * backoff per Jev's own API guidance ("retry with exponential backoff instead of retrying
 * immediately"), unlike the fixed-wait retry used for the LLM adapter.
 */
@Validated
@ConfigurationProperties(prefix = "joblens.resilience.jev")
public record JevResilienceProperties(
        @Min(1) int retryMaxAttempts,
        @NotNull Duration retryInitialInterval,
        @Min(1) double retryBackoffMultiplier,
        @Min(2) int circuitBreakerSlidingWindowSize,
        @Min(1) int circuitBreakerMinimumCalls,
        @Min(1) float circuitBreakerFailureRateThreshold,
        @NotNull Duration circuitBreakerWaitDurationInOpenState) {
}
