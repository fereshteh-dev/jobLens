package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "joblens.cache")
public record CacheProperties(@Min(1) long maxSize, @NotNull Duration expireAfterWrite) {
}
