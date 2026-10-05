package com.app.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.authservice.entity.Role;
import com.app.authservice.entity.User;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mysecretkeymysecretkeymysecretkeymysecretkey");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60_000L);
        jwtUtil.init();
    }

    @Test
    void generateAndExtractTokenValues() {
        User user = User.builder()
                .userId(9L)
                .email("reader@example.com")
                .role(Role.READER)
                .build();

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("reader@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("READER");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(9L);
        assertThat(jwtUtil.extractExpiry(token)).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertThat(jwtUtil.validateToken("bad-token")).isFalse();
    }
}
