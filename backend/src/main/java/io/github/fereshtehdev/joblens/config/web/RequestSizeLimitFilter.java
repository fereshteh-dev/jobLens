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

/**
 * Rejects requests whose declared {@code Content-Length} exceeds
 * {@code joblens.security.max-request-body-size}, before the body is read. Content-Length
 * based, not a streaming byte-counter - sufficient for this app's threat model (an
 * accidentally or deliberately oversized JSON body), not a hardened defense against a
 * chunked-encoding client that omits Content-Length entirely.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestSizeLimitFilter(SecurityProperties properties) {
        this.maxBytes = properties.maxRequestBodySize().toBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            response.setStatus(413);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Payload Too Large","status":413,\
                    "detail":"Request body exceeds the maximum allowed size."}""");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
