package com.app.commentservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

    @Test
    void shouldSkipFrameworkPaths() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/v3/api-docs"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/comments"))).isFalse();
    }

    @Test
    void doFilterInternalRunsChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/comments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
