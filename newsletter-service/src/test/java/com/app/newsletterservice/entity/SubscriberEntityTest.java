package com.app.newsletterservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SubscriberEntityTest {

    @Test
    void prePersistAppliesDefaults() {
        Subscriber subscriber = Subscriber.builder().build();

        subscriber.prePersist();

        assertNotNull(subscriber.getSubscribedAt());
        assertEquals(SubscriberStatus.PENDING, subscriber.getStatus());
    }
}

