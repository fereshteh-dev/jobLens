package io.github.fereshtehdev.joblens.config.web;

import io.github.fereshtehdev.joblens.config.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates the extension's {@code X-API-Key} header against the configured key(s), using
 * {@link MessageDigest#isEqual} (the JDK's constant-time byte comparison) so response timing
 * doesn't leak how much of a guessed key was correct. Only active when
 * {@code joblens.security.api-key.enabled=true} (default false - see SecurityProperties).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final SecurityProperties.ApiKey properties;

    public ApiKeyAuthFilter(SecurityProperties properties) {
        this.properties = properties.apiKey();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);
        if (providedKey != null && matchesAnyConfiguredKey(providedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(401);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,\
                "detail":"A valid X-API-Key header is required."}""");
    }

    private boolean matchesAnyConfiguredKey(String providedKey) {
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        boolean matched = false;
        // Deliberately not short-circuiting on the first match, so the number of configured
        // keys checked doesn't itself become a timing signal.
        for (String validKey : properties.validKeys()) {
            if (MessageDigest.isEqual(provided, validKey.getBytes(StandardCharsets.UTF_8))) {
                matched = true;
            }
        }
        return matched;
    }
}
