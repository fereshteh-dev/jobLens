package io.github.fereshtehdev.joblens.config.web;

import io.github.fereshtehdev.joblens.config.properties.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void underLimit_passesThrough() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(true, 2));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void overLimit_returns429ProblemJson() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(true, 1));
        MockHttpServletRequest first = new MockHttpServletRequest();
        first.addHeader("X-API-Key", "test-key");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.addHeader("X-API-Key", "test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(second, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void disabled_alwaysPassesThrough() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(false, 1));

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void differentApiKeys_haveIndependentBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(true, 1));
        MockHttpServletRequest first = new MockHttpServletRequest();
        first.addHeader("X-API-Key", "key-a");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.addHeader("X-API-Key", "key-b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(second, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
