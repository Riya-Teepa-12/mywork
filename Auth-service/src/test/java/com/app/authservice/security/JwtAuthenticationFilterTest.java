package com.app.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsSwaggerPaths() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsAuthenticationForValidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        when(jwtUtil.validateToken("token-1")).thenReturn(true);
        when(jwtUtil.extractEmail("token-1")).thenReturn("reader@example.com");
        when(jwtUtil.extractRole("token-1")).thenReturn("READER");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("reader@example.com");
    }

    @Test
    void leavesContextEmptyForInvalidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        when(jwtUtil.validateToken("token-2")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
