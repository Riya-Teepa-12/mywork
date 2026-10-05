package com.app.mediaservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MediaEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Media media = Media.builder().build();

        media.prePersist();

        assertNotNull(media.getUploadedAt());
        assertEquals(false, media.getIsDeleted());
    }
}

