package io.github.fereshtehdev.joblens.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

/**
 * API-key auth, CORS and request-size limits - deferred from Phase 3 (see ADR-0005) since
 * they need a real client (the extension) to authenticate against. {@code apiKey.enabled} and
 * {@code allowedOrigins} default to off/empty, matching the same opt-in pattern already used
 * for {@code joblens.ai.enabled}/{@code joblens.jev.enabled}: turn them on once you have a
 * real key and a real extension origin to configure (see the extension's options page).
 */
@Validated
@ConfigurationProperties(prefix = "joblens.security")
public record SecurityProperties(ApiKey apiKey, List<String> allowedOrigins, @NotNull DataSize maxRequestBodySize) {

    public SecurityProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public record ApiKey(boolean enabled, Set<String> validKeys) {

        public ApiKey {
            validKeys = validKeys == null ? Set.of() : Set.copyOf(validKeys);
        }
    }
}
