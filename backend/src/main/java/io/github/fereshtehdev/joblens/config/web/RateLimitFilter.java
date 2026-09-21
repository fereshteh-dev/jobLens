package io.github.fereshtehdev.joblens.config.web;

import io.github.bucket4j.Bucket;
import io.github.fereshtehdev.joblens.config.properties.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key request-rate limiting. Keyed by the {@code X-API-Key} header when present, else the
 * client's remote address - see ADR-0005: since there's no real API-key authentication yet
 * (Phase 5), this is rate-limiting infrastructure, not yet abuse protection (a caller can
 * bypass their own limit by rotating the header value).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(rateLimitKey(request), key -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Rate limit exceeded. Try again later."}""");
    }

    private Bucket newBucket() {
        int limit = properties.requestsPerMinute();
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth.capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)))
                .build();
    }

    private static String rateLimitKey(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        return (apiKey != null && !apiKey.isBlank()) ? "key:" + apiKey : "ip:" + request.getRemoteAddr();
    }
}
