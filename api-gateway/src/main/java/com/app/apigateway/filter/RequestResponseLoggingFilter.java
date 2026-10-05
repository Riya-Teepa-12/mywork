package com.app.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (shouldSkip(path)) {
            return chain.filter(exchange);
        }
        long start = System.currentTimeMillis();
        String method = String.valueOf(exchange.getRequest().getMethod());
        return chain.filter(exchange)
                .doOnError(ex -> log.error("{} {} failed", method, path, ex))
                .doFinally(signalType -> {
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    long elapsedMs = System.currentTimeMillis() - start;
                    int statusCode = status == null ? 0 : status.value();
                    log.info("{} {} -> {} ({} ms)", method, path, statusCode, elapsedMs);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean shouldSkip(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
