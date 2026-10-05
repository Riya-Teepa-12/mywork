package com.app.postservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PostEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Post post = Post.builder().build();

        post.prePersist();

        assertNotNull(post.getCreatedAt());
        assertNotNull(post.getUpdatedAt());
        assertEquals(PostStatus.DRAFT, post.getStatus());
        assertEquals(1, post.getReadTimeMin());
        assertEquals(0L, post.getViewCount());
        assertEquals(0L, post.getLikesCount());
        assertFalse(post.getFeatured());
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        Post post = Post.builder().updatedAt(LocalDateTime.now().minusDays(1)).build();

        post.preUpdate();

        assertNotNull(post.getUpdatedAt());
        assertTrue(post.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }
}

