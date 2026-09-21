package io.github.fereshtehdev.joblens.config.web;

import io.github.fereshtehdev.joblens.config.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthFilterTest {

    @Test
    void disabled_alwaysPassesThrough() throws Exception {
        ApiKeyAuthFilter filter = filter(false, Set.of("real-key"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enabled_missingHeader_returns401() throws Exception {
        ApiKeyAuthFilter filter = filter(true, Set.of("real-key"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
    }

    @Test
    void enabled_wrongKey_returns401() throws Exception {
        ApiKeyAuthFilter filter = filter(true, Set.of("real-key"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void enabled_correctKey_passesThrough() throws Exception {
        ApiKeyAuthFilter filter = filter(true, Set.of("real-key"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "real-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enabled_matchesAnyOfMultipleConfiguredKeys() throws Exception {
        ApiKeyAuthFilter filter = filter(true, Set.of("key-a", "key-b"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "key-b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static ApiKeyAuthFilter filter(boolean enabled, Set<String> validKeys) {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.ApiKey(enabled, validKeys), null, DataSize.ofKilobytes(50));
        return new ApiKeyAuthFilter(properties);
    }
}
