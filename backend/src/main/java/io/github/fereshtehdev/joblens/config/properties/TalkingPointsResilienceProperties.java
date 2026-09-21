package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Retry/circuit-breaker tuning for {@code SpringAiTalkingPointsWriter}, decorated by hand with
 * Resilience4j's framework-agnostic core modules - see ADR-0005 for why not the Spring Boot
 * integration module.
 */
@Validated
@ConfigurationProperties(prefix = "joblens.resilience.talking-points")
public record TalkingPointsResilienceProperties(
        @Min(1) int retryMaxAttempts,
        @NotNull Duration retryWaitDuration,
        @Min(2) int circuitBreakerSlidingWindowSize,
        @Min(1) int circuitBreakerMinimumCalls,
        @Min(1) float circuitBreakerFailureRateThreshold,
        @NotNull Duration circuitBreakerWaitDurationInOpenState) {
}
