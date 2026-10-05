package com.app.notificationservice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.app.notificationservice.entity.NotificationType;

class NotificationDtoCoverageTest {

    @Test
    void notificationResponseBuilderAndAccessorsWork() {
        LocalDateTime now = LocalDateTime.now();
        NotificationResponse response = NotificationResponse.builder()
                .notificationId(1L)
                .recipientId(10L)
                .actorId(20L)
                .type(NotificationType.NEW_POST)
                .title("Title")
                .message("Message")
                .relatedId(99L)
                .relatedType("POST")
                .read(true)
                .createdAt(now)
                .build();

        assertThat(response.getNotificationId()).isEqualTo(1L);
        assertThat(response.getRecipientId()).isEqualTo(10L);
        assertThat(response.getActorId()).isEqualTo(20L);
        assertThat(response.getType()).isEqualTo(NotificationType.NEW_POST);
        assertThat(response.getTitle()).isEqualTo("Title");
        assertThat(response.getMessage()).isEqualTo("Message");
        assertThat(response.getRelatedId()).isEqualTo(99L);
        assertThat(response.getRelatedType()).isEqualTo("POST");
        assertThat(response.getRead()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void dispatchResponseAccessorsWork() {
        DispatchResponse response = new DispatchResponse("ok", 2);

        assertThat(response.getMessage()).isEqualTo("ok");
        assertThat(response.getCount()).isEqualTo(2L);

        response.setMessage("updated");
        response.setCount(3L);
        assertThat(response.getMessage()).isEqualTo("updated");
        assertThat(response.getCount()).isEqualTo(3L);
    }
}
