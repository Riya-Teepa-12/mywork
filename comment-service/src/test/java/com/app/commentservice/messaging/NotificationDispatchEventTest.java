package com.app.commentservice.messaging;

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
                .sourceService("comment-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("MENTION")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("COMMENT")
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
    void equalityBranchesAreExercised() {
        NotificationDispatchEvent base = sample();
        NotificationDispatchEvent peer = sample();
        assertThat(base)
                .isEqualTo(peer)
                .hasSameHashCodeAs(peer)
                .isEqualTo(base)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object());

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

        assertThat(base.toString()).contains("comment-service");
        assertThat(new NotificationDispatchEvent()).hasSameHashCodeAs(new NotificationDispatchEvent());
    }

    private static final class ChildEvent extends NotificationDispatchEvent {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }

    private NotificationDispatchEvent sample() {
        return NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("comment-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("MENTION")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("COMMENT")
                .occurredAt(Instant.parse("2025-01-02T00:00:00Z"))
                .build();
    }

    private List<NotificationDispatchEvent> changedFieldVariants() {
        NotificationDispatchEvent recipientId = sample();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = sample();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent occurredAt = sample();
        occurredAt.setOccurredAt(Instant.parse("2025-02-01T00:00:00Z"));
        NotificationDispatchEvent title = sample();
        title.setTitle("different");
        NotificationDispatchEvent message = sample();
        message.setMessage("different");
        NotificationDispatchEvent channel = sample();
        channel.setDispatchChannel("EMAIL");
        return List.of(recipientId, recipientIds, occurredAt, title, message, channel);
    }

    private List<NotificationDispatchEvent> nullMismatchVariants() {
        NotificationDispatchEvent eventId = sample();
        eventId.setEventId(null);
        NotificationDispatchEvent sourceService = sample();
        sourceService.setSourceService(null);
        NotificationDispatchEvent channel = sample();
        channel.setDispatchChannel(null);
        NotificationDispatchEvent recipientId = sample();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = sample();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent actor = sample();
        actor.setActorId(null);
        NotificationDispatchEvent type = sample();
        type.setType(null);
        NotificationDispatchEvent title = sample();
        title.setTitle(null);
        NotificationDispatchEvent message = sample();
        message.setMessage(null);
        NotificationDispatchEvent relatedId = sample();
        relatedId.setRelatedId(null);
        NotificationDispatchEvent relatedType = sample();
        relatedType.setRelatedType(null);
        NotificationDispatchEvent occurredAt = sample();
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
