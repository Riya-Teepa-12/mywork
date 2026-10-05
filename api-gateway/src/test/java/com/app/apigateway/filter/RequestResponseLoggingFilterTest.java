package com.app.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

class RequestResponseLoggingFilterTest {

    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();

    @Test
    void getOrderMatchesLowestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void skipsFrameworkPaths() {
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, chain).block();

        assertThat(called.get()).isTrue();
    }

    @Test
    void logsCompletionForNonSkippedPath() {
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/posts/private").build());

        filter.filter(exchange, chain).block();

        assertThat(called.get()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void propagatesChainErrors() {
        GatewayFilterChain chain = exchange -> Mono.error(new IllegalStateException("boom"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/posts/private").build());
        Mono<Void> result = filter.filter(exchange, chain);

        assertThatThrownBy(result::block)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }
}
