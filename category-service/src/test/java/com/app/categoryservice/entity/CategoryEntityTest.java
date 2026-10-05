package com.app.categoryservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CategoryEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Category category = Category.builder().build();

        category.prePersist();

        assertNotNull(category.getCreatedAt());
        assertEquals(0L, category.getPostCount());
    }

    @Test
    void tagAndLinkEntitiesAreSimpleDataHolders() {
        Tag tag = Tag.builder().name("Java").slug("java").postCount(1L).createdAt(LocalDateTime.now()).build();
        PostCategory postCategory = PostCategory.builder().postId(1L).categoryId(2L).build();
        PostTag postTag = PostTag.builder().postId(3L).tagId(4L).build();

        assertEquals("Java", tag.getName());
        assertEquals(1L, postCategory.getPostId());
        assertEquals(4L, postTag.getTagId());
    }
}

