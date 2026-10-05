package com.app.commentservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommentEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Comment comment = Comment.builder().build();

        comment.prePersist();

        assertNotNull(comment.getCreatedAt());
        assertNotNull(comment.getUpdatedAt());
        assertEquals(0L, comment.getLikesCount());
        assertEquals(CommentStatus.APPROVED, comment.getStatus());
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        Comment comment = Comment.builder().updatedAt(LocalDateTime.now().minusDays(1)).build();

        comment.preUpdate();

        assertNotNull(comment.getUpdatedAt());
    }
}

