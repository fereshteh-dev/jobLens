package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joblens.rate-limit")
public record RateLimitProperties(boolean enabled, @Min(1) int requestsPerMinute) {
}
