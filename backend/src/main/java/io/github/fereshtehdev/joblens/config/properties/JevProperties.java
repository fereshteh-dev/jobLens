package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joblens.jev")
public record JevProperties(@NotBlank String baseUrl, @NotBlank String apiKey, @NotBlank String model) {
}
