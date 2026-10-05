package com.app.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.app.apigateway.security.JwtUtil;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Test
    void allowsOptionsAndPublicPaths() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            captured.set(exchange);
            return Mono.empty();
        };

        MockServerWebExchange options = MockServerWebExchange.from(
                MockServerHttpRequest.options("/posts").build());
        filter.filter(options, chain).block();
        assertThat(captured.get()).isNotNull();

        captured.set(null);
        MockServerWebExchange publicGet = MockServerWebExchange.from(
                MockServerHttpRequest.get("/posts/published").build());
        filter.filter(publicGet, chain).block();
        assertThat(captured.get()).isNotNull();
    }

    @Test
    void rejectsMissingOrInvalidBearerToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        GatewayFilterChain chain = exchange -> Mono.empty();

        MockServerWebExchange missingHeader = MockServerWebExchange.from(
                MockServerHttpRequest.get("/posts/private").build());
        filter.filter(missingHeader, chain).block();
        assertThat(missingHeader.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        MockServerWebExchange invalid = MockServerWebExchange.from(
                MockServerHttpRequest.get("/posts/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build());
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);
        filter.filter(invalid, chain).block();
        assertThat(invalid.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void injectsUserHeadersWhenTokenIsValid() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            captured.set(exchange);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/posts/private")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                        .build());

        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.extractEmail("good-token")).thenReturn("reader@example.com");
        when(jwtUtil.extractRole("good-token")).thenReturn("READER");
        when(jwtUtil.extractUserId("good-token")).thenReturn(77L);

        filter.filter(exchange, chain).block();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("reader@example.com");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("READER");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("77");
    }
}
