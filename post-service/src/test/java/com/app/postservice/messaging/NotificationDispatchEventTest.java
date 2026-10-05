package com.app.postservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class NotificationDispatchEventTest {

    @Test
    void builderAndAccessorsWork() {
        Instant now = Instant.now();
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("post-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("NEW_POST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("POST")
                .occurredAt(now)
                .build();

        assertThat(event.getEventId()).isEqualTo("evt-1");
        assertThat(event.getRecipientIds()).containsExactly(10L, 11L);
        assertThat(event.getOccurredAt()).isEqualTo(now);

        NotificationDispatchEvent mutable = new NotificationDispatchEvent();
        mutable.setDispatchChannel("EMAIL");
        mutable.setRecipientId(99L);
        assertThat(mutable.getDispatchChannel()).isEqualTo("EMAIL");
        assertThat(mutable.getRecipientId()).isEqualTo(99L);
    }

    @Test
    void equalsHashCodeAndToStringWorkAcrossFieldChanges() {
        NotificationDispatchEvent first = event();
        NotificationDispatchEvent second = event();
        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .isEqualTo(first)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object());

        assertAllNotEqual(first, changedFieldVariants());
        assertAllNotEqualBothWays(first, nullMismatchVariants());

        NotificationDispatchEvent child = new ChildEvent();
        child.setEventId(first.getEventId());
        child.setSourceService(first.getSourceService());
        child.setDispatchChannel(first.getDispatchChannel());
        child.setRecipientId(first.getRecipientId());
        child.setRecipientIds(first.getRecipientIds());
        child.setActorId(first.getActorId());
        child.setType(first.getType());
        child.setTitle(first.getTitle());
        child.setMessage(first.getMessage());
        child.setRelatedId(first.getRelatedId());
        child.setRelatedType(first.getRelatedType());
        child.setOccurredAt(first.getOccurredAt());
        assertThat(first).isNotEqualTo(child).isNotEqualTo(new NotificationDispatchEvent());

        assertThat(first.toString()).contains("post-service");
        assertThat(new NotificationDispatchEvent()).hasSameHashCodeAs(new NotificationDispatchEvent());
    }

    private static final class ChildEvent extends NotificationDispatchEvent {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }

    private NotificationDispatchEvent event() {
        return NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("post-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("NEW_POST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("POST")
                .occurredAt(Instant.parse("2025-01-05T00:00:00Z"))
                .build();
    }

    private List<NotificationDispatchEvent> changedFieldVariants() {
        NotificationDispatchEvent title = event();
        title.setTitle("another");
        NotificationDispatchEvent message = event();
        message.setMessage("another");
        NotificationDispatchEvent recipientIds = event();
        recipientIds.setRecipientIds(List.of(22L));
        NotificationDispatchEvent recipientId = event();
        recipientId.setRecipientId(22L);
        NotificationDispatchEvent occurredAt = event();
        occurredAt.setOccurredAt(Instant.parse("2025-01-04T00:00:00Z"));
        NotificationDispatchEvent channel = event();
        channel.setDispatchChannel("EMAIL");
        return List.of(title, message, recipientIds, recipientId, occurredAt, channel);
    }

    private List<NotificationDispatchEvent> nullMismatchVariants() {
        NotificationDispatchEvent eventId = event();
        eventId.setEventId(null);
        NotificationDispatchEvent sourceService = event();
        sourceService.setSourceService(null);
        NotificationDispatchEvent channel = event();
        channel.setDispatchChannel(null);
        NotificationDispatchEvent recipientId = event();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = event();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent actor = event();
        actor.setActorId(null);
        NotificationDispatchEvent type = event();
        type.setType(null);
        NotificationDispatchEvent title = event();
        title.setTitle(null);
        NotificationDispatchEvent message = event();
        message.setMessage(null);
        NotificationDispatchEvent relatedId = event();
        relatedId.setRelatedId(null);
        NotificationDispatchEvent relatedType = event();
        relatedType.setRelatedType(null);
        NotificationDispatchEvent occurredAt = event();
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
