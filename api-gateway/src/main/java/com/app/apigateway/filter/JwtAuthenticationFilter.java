package com.app.apigateway.filter;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.app.apigateway.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_ANY_METHOD_PATH_PREFIXES = List.of(
            "/auth/register",
            "/auth/register/request-otp",
            "/auth/register/verify-otp",
            "/auth/login",
            "/auth/refresh",
            "/auth/oauth/login",
            "/auth/validate",
            "/auth/public/users/",
            "/auth/password/forgot/request-otp",
            "/auth/password/forgot/reset",
            "/auth/subscriptions/plans",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/newsletter/subscribe",
            "/newsletter/confirm",
            "/newsletter/unsubscribe",
            "/auth/v3/api-docs",
            "/posts/v3/api-docs",
            "/comments/v3/api-docs",
            "/categories/v3/api-docs",
            "/media/v3/api-docs",
            "/newsletter/v3/api-docs",
            "/notifications/v3/api-docs",
            "/webjars",
            "/actuator");

    private static final List<String> PUBLIC_GET_PATH_PREFIXES = List.of(
            "/posts/published",
            "/posts/slug/",
            "/posts/search",
            "/categories",
            "/tags");

    private static final List<Pattern> PUBLIC_GET_PATH_PATTERNS = List.of(
            Pattern.compile("^/comments/post/\\d+$"),
            Pattern.compile("^/posts/author/\\d+/published$"),
            Pattern.compile("^/comments/\\d+$"),
            Pattern.compile("^/comments/\\d+/replies$"),
            Pattern.compile("^/comments/count$"),
            Pattern.compile("^/media/post/\\d+$"),
            Pattern.compile("^/media/\\d+$"));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path, exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.set("X-User-Email", jwtUtil.extractEmail(token));
                    headers.set("X-User-Role", jwtUtil.extractRole(token));
                    Long userId = jwtUtil.extractUserId(token);
                    if (userId != null) {
                        headers.set("X-User-Id", String.valueOf(userId));
                    }
                }))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        for (String prefix : PUBLIC_ANY_METHOD_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        if (HttpMethod.GET.equals(method)) {
            for (String prefix : PUBLIC_GET_PATH_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
            for (Pattern pattern : PUBLIC_GET_PATH_PATTERNS) {
                if (pattern.matcher(path).matches()) {
                    return true;
                }
            }
        }
        return false;
    }
}
