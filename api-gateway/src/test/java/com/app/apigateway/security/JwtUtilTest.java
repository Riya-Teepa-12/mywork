package com.app.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "mysecretkeymysecretkeymysecretkeymysecretkey";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    @Test
    void extractsClaimsAndValidatesNonExpiredToken() {
        String token = Jwts.builder()
                .setSubject("reader@example.com")
                .claim("role", "READER")
                .claim("userId", 11)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("reader@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("READER");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(11L);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForExpiredOrInvalidTokens() {
        String expired = Jwts.builder()
                .setSubject("reader@example.com")
                .setExpiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.validateToken(expired)).isFalse();
        assertThat(jwtUtil.validateToken("invalid")).isFalse();
    }

    @Test
    void constructorRejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtUtil("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.jwt.secret");
    }
}
