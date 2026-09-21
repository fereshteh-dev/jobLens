package io.github.fereshtehdev.joblens.config;

import io.github.fereshtehdev.joblens.config.properties.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Strict CORS for the extension's origin only - no wildcard. Empty {@code allowedOrigins}
 * (the default) means no CORS headers are added at all, so a browser blocks any cross-origin
 * call until the operator configures their real {@code chrome-extension://<id>} origin.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityProperties properties;

    public WebConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (properties.allowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(properties.allowedOrigins().toArray(new String[0]))
                .allowedMethods("POST")
                .allowedHeaders("Content-Type", "X-API-Key")
                .maxAge(3600);
    }
}
