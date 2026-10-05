package com.app.authservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AuthorUpgradeRequestEntityTest {

    @Test
    void onCreateInitializesDefaults() {
        AuthorUpgradeRequest request = AuthorUpgradeRequest.builder()
                .userId(1L)
                .username("writer")
                .email("writer@example.com")
                .bio("bio")
                .motivation("motivation")
                .expertiseCategories("java")
                .writingSampleUrls("https://example.com")
                .build();

        request.onCreate();

        assertThat(request.getCreatedAt()).isNotNull();
        assertThat(request.getUpdatedAt()).isNotNull();
        assertThat(request.getStatus()).isEqualTo(AuthorUpgradeStatus.PENDING);
    }

    @Test
    void onUpdateRefreshesUpdatedTimestamp() {
        AuthorUpgradeRequest request = AuthorUpgradeRequest.builder()
                .userId(1L)
                .username("writer")
                .email("writer@example.com")
                .bio("bio")
                .motivation("motivation")
                .expertiseCategories("java")
                .writingSampleUrls("https://example.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .status(AuthorUpgradeStatus.PENDING)
                .build();

        request.onUpdate();

        assertThat(request.getUpdatedAt()).isAfter(request.getCreatedAt());
    }
}
