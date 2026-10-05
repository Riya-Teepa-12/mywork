package com.app.notificationservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NotificationEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Notification notification = Notification.builder().build();

        notification.prePersist();

        assertNotNull(notification.getCreatedAt());
        assertEquals(false, notification.getIsRead());
    }
}

