package com.app.notificationservice.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationKafkaConsumer consumer;

    @Test
    void consumeInShadowModeDoesNotTriggerSideEffects() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", false);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("post-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .actorId(20L)
                .type("NEW_POST")
                .title("t")
                .message("m")
                .build();

        consumer.consume(event);

        verify(notificationService, never()).send(any());
        verify(notificationService, never()).sendBulk(any());
        verify(notificationService, never()).sendEmail(any());
    }

    @Test
    void consumeEmailEventDispatchesEmailWhenEnabled() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-2")
                .sourceService("comment-service")
                .dispatchChannel("EMAIL")
                .recipientId(99L)
                .actorId(1L)
                .type("MENTION")
                .title("title")
                .message("message")
                .build();

        consumer.consume(event);

        verify(notificationService).sendEmail(any());
        verify(notificationService, never()).send(any());
    }

    @Test
    void consumeBulkInAppEventDispatchesBulkWhenEnabled() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-3")
                .sourceService("post-service")
                .dispatchChannel("IN_APP")
                .recipientIds(List.of(1L, 2L))
                .actorId(7L)
                .type("NEW_POST")
                .title("title")
                .message("message")
                .build();

        consumer.consume(event);

        verify(notificationService).sendBulk(any());
        verify(notificationService, never()).send(any());
    }

    @Test
    void consumeSkipsUnknownNotificationType() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-4")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .type("NOT_A_REAL_TYPE")
                .build();

        consumer.consume(event);

        verify(notificationService, never()).send(any());
        verify(notificationService, never()).sendBulk(any());
        verify(notificationService, never()).sendEmail(any());
    }

    @Test
    void consumeSkipsSingleDispatchWhenRecipientMissing() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-5")
                .dispatchChannel("IN_APP")
                .type("NEW_POST")
                .build();

        consumer.consume(event);

        verify(notificationService, never()).send(any());
    }

    @Test
    void consumeSingleInAppEventDispatchesSendWhenEnabled() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-6")
                .dispatchChannel("IN_APP")
                .recipientId(77L)
                .actorId(1L)
                .type("LIKE")
                .title("title")
                .message("message")
                .build();

        consumer.consume(event);

        verify(notificationService).send(any());
        verify(notificationService, never()).sendBulk(any());
    }

    @Test
    void consumeSkipsEmailDispatchWhenRecipientMissing() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-7")
                .dispatchChannel("EMAIL")
                .type("MENTION")
                .build();

        consumer.consume(event);

        verify(notificationService, never()).sendEmail(any());
    }

    @Test
    void consumeHandlesNotificationServiceFailureWithoutThrowing() {
        ReflectionTestUtils.setField(consumer, "applySideEffects", true);
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-8")
                .dispatchChannel("IN_APP")
                .recipientId(42L)
                .type("NEW_COMMENT")
                .build();
        doThrow(new RuntimeException("boom")).when(notificationService).send(any());

        consumer.consume(event);

        verify(notificationService).send(any());
    }
}
