package com.app.authservice.messaging;

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
                .sourceService("auth-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("ADMIN_BROADCAST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("AUTHOR_REQUEST")
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
    void equalsHashCodeAndToStringCoverBranches() {
        NotificationDispatchEvent base = populatedEvent();
        NotificationDispatchEvent same = populatedEvent();
        assertThat(base)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isEqualTo(base)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object());
        assertThat(base.toString()).contains("evt-1");

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
    }

    private static final class ChildEvent extends NotificationDispatchEvent {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }

    private NotificationDispatchEvent populatedEvent() {
        return NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("auth-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("ADMIN_BROADCAST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("AUTHOR_REQUEST")
                .occurredAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
    }

    private List<NotificationDispatchEvent> changedFieldVariants() {
        NotificationDispatchEvent eventId = populatedEvent();
        eventId.setEventId("evt-2");
        NotificationDispatchEvent sourceService = populatedEvent();
        sourceService.setSourceService("x");
        NotificationDispatchEvent channel = populatedEvent();
        channel.setDispatchChannel("EMAIL");
        NotificationDispatchEvent recipientId = populatedEvent();
        recipientId.setRecipientId(99L);
        NotificationDispatchEvent recipientIds = populatedEvent();
        recipientIds.setRecipientIds(List.of(9L));
        NotificationDispatchEvent actor = populatedEvent();
        actor.setActorId(2L);
        NotificationDispatchEvent type = populatedEvent();
        type.setType("NEW_POST");
        NotificationDispatchEvent title = populatedEvent();
        title.setTitle("other");
        NotificationDispatchEvent message = populatedEvent();
        message.setMessage("other");
        NotificationDispatchEvent relatedId = populatedEvent();
        relatedId.setRelatedId(11L);
        NotificationDispatchEvent relatedType = populatedEvent();
        relatedType.setRelatedType("POST");
        NotificationDispatchEvent occurredAt = populatedEvent();
        occurredAt.setOccurredAt(Instant.parse("2025-01-01T01:00:00Z"));
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

    private List<NotificationDispatchEvent> nullMismatchVariants() {
        NotificationDispatchEvent eventId = populatedEvent();
        eventId.setEventId(null);
        NotificationDispatchEvent sourceService = populatedEvent();
        sourceService.setSourceService(null);
        NotificationDispatchEvent channel = populatedEvent();
        channel.setDispatchChannel(null);
        NotificationDispatchEvent recipientId = populatedEvent();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = populatedEvent();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent actor = populatedEvent();
        actor.setActorId(null);
        NotificationDispatchEvent type = populatedEvent();
        type.setType(null);
        NotificationDispatchEvent title = populatedEvent();
        title.setTitle(null);
        NotificationDispatchEvent message = populatedEvent();
        message.setMessage(null);
        NotificationDispatchEvent relatedId = populatedEvent();
        relatedId.setRelatedId(null);
        NotificationDispatchEvent relatedType = populatedEvent();
        relatedType.setRelatedType(null);
        NotificationDispatchEvent occurredAt = populatedEvent();
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
