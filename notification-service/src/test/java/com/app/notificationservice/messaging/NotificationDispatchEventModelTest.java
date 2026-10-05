package com.app.notificationservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class NotificationDispatchEventModelTest {

    @Test
    void eventContractsAreExercised() {
        NotificationDispatchEvent base = create();
        NotificationDispatchEvent same = create();
        assertThat(base)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isEqualTo(base)
                .isNotNull()
                .isNotEqualTo(new Object());
        assertThat(base.toString()).contains("notification-service");

        assertAllNotEqual(base, changedFieldVariants());
        assertAllNotEqualBothWays(base, nullMismatchVariants());

        NotificationDispatchEvent child = new ChildEvent();
        child.setEventId(base.getEventId());
        child.setSourceService(base.getSourceService());
        child.setDispatchChannel(base.getDispatchChannel());
        child.setRecipientId(base.getRecipientId());
        child.setRecipientIds(base.getRecipientIds());
        child.setActorId(base.getActorId());
        child.setType(base.getType());
        child.setTitle(base.getTitle());
        child.setMessage(base.getMessage());
        child.setRelatedId(base.getRelatedId());
        child.setRelatedType(base.getRelatedType());
        child.setOccurredAt(base.getOccurredAt());
        assertThat(base).isNotEqualTo(child).isNotEqualTo(new NotificationDispatchEvent());
        assertThat(new NotificationDispatchEvent()).hasSameHashCodeAs(new NotificationDispatchEvent());
        assertThat(base).isNotNull();
    }

    private static final class ChildEvent extends NotificationDispatchEvent {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }

    private NotificationDispatchEvent create() {
        return NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("notification-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("NEW_COMMENT")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("COMMENT")
                .occurredAt(Instant.parse("2025-01-06T00:00:00Z"))
                .build();
    }

    private List<NotificationDispatchEvent> changedFieldVariants() {
        NotificationDispatchEvent eventId = create();
        eventId.setEventId("evt-2");
        NotificationDispatchEvent type = create();
        type.setType("LIKE");
        NotificationDispatchEvent relatedType = create();
        relatedType.setRelatedType("POST");
        NotificationDispatchEvent occurredAt = create();
        occurredAt.setOccurredAt(Instant.parse("2025-01-06T01:00:00Z"));
        NotificationDispatchEvent recipientId = create();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = create();
        recipientIds.setRecipientIds(null);
        return List.of(eventId, type, relatedType, occurredAt, recipientId, recipientIds);
    }

    private List<NotificationDispatchEvent> nullMismatchVariants() {
        NotificationDispatchEvent eventId = create();
        eventId.setEventId(null);
        NotificationDispatchEvent sourceService = create();
        sourceService.setSourceService(null);
        NotificationDispatchEvent channel = create();
        channel.setDispatchChannel(null);
        NotificationDispatchEvent recipientId = create();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = create();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent actor = create();
        actor.setActorId(null);
        NotificationDispatchEvent type = create();
        type.setType(null);
        NotificationDispatchEvent title = create();
        title.setTitle(null);
        NotificationDispatchEvent message = create();
        message.setMessage(null);
        NotificationDispatchEvent relatedId = create();
        relatedId.setRelatedId(null);
        NotificationDispatchEvent relatedType = create();
        relatedType.setRelatedType(null);
        NotificationDispatchEvent occurredAt = create();
        occurredAt.setOccurredAt(null);
        return List.of(
                eventId,
                sourceService,
                channel,
                recipientId,
                recipientIds,
                actor,
                type,
                title,
                message,
                relatedId,
                relatedType,
                occurredAt);
    }

    private void assertAllNotEqual(NotificationDispatchEvent base, List<NotificationDispatchEvent> variants) {
        for (NotificationDispatchEvent variant : variants) {
            assertThat(base).isNotEqualTo(variant);
        }
    }

    private void assertAllNotEqualBothWays(NotificationDispatchEvent base, List<NotificationDispatchEvent> variants) {
        for (NotificationDispatchEvent variant : variants) {
            assertThat(base).isNotEqualTo(variant);
            assertThat(variant).isNotEqualTo(base);
        }
    }
}
