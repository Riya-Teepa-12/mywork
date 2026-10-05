package com.app.notificationservice.messaging;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.entity.NotificationType;
import com.app.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationService notificationService;

    @Value("${inkwell.notification.kafka.consumer.apply-side-effects:false}")
    private boolean applySideEffects;

    @KafkaListener(topics = "${inkwell.notification.kafka.topic:notification.dispatch.v1}")
    public void consume(NotificationDispatchEvent event) {
        if (event == null) {
            return;
        }
        if (!applySideEffects) {
            log.info(
                    "Kafka shadow event received. eventId={}, sourceService={}, channel={}, recipientId={}, recipientCount={}",
                    event.getEventId(),
                    event.getSourceService(),
                    event.getDispatchChannel(),
                    event.getRecipientId(),
                    event.getRecipientIds() == null ? 0 : event.getRecipientIds().size());
            return;
        }

        NotificationType notificationType = toNotificationType(event.getType());
        if (notificationType == null) {
            log.warn(
                    "Skipping Kafka notification event with unknown type. eventId={}, type={}",
                    event.getEventId(),
                    event.getType());
            return;
        }

        boolean isEmail = "EMAIL".equalsIgnoreCase(event.getDispatchChannel());
        if (isEmail) {
            dispatchEmail(event, notificationType);
            return;
        }

        if (event.getRecipientIds() != null && !event.getRecipientIds().isEmpty()) {
            dispatchBulk(event, notificationType);
            return;
        }

        if (event.getRecipientId() == null) {
            log.warn("Skipping Kafka notification event with empty recipient. eventId={}", event.getEventId());
            return;
        }

        try {
            notificationService.send(toNotificationRequest(event, notificationType, event.getRecipientId()));
        } catch (Exception ex) {
            log.warn("Failed to process Kafka notification event {}", event.getEventId(), ex);
        }
    }

    private void dispatchBulk(NotificationDispatchEvent event, NotificationType type) {
        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            return;
        }
        BulkNotificationRequest request = new BulkNotificationRequest();
        request.setRecipientIds(event.getRecipientIds());
        request.setActorId(event.getActorId());
        request.setType(type);
        request.setTitle(event.getTitle());
        request.setMessage(event.getMessage());
        request.setRelatedId(event.getRelatedId());
        request.setRelatedType(event.getRelatedType());
        try {
            notificationService.sendBulk(request);
        } catch (Exception ex) {
            log.warn("Failed to process Kafka bulk notification event {}", event.getEventId(), ex);
        }
    }

    private void dispatchEmail(NotificationDispatchEvent event, NotificationType type) {
        if (event.getRecipientId() == null) {
            log.warn("Skipping Kafka email event with empty recipient. eventId={}", event.getEventId());
            return;
        }
        try {
            notificationService.sendEmail(toNotificationRequest(event, type, event.getRecipientId()));
        } catch (Exception ex) {
            log.warn("Failed to process Kafka email notification event {}", event.getEventId(), ex);
        }
    }

    private NotificationRequest toNotificationRequest(NotificationDispatchEvent event, NotificationType type, Long recipientId) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(recipientId);
        request.setActorId(event.getActorId());
        request.setType(type);
        request.setTitle(event.getTitle());
        request.setMessage(event.getMessage());
        request.setRelatedId(event.getRelatedId());
        request.setRelatedType(event.getRelatedType());
        return request;
    }

    private NotificationType toNotificationType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return NotificationType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
