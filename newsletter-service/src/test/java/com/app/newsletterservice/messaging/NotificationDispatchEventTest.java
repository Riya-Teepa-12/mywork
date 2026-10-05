package com.app.newsletterservice.messaging;

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
                .sourceService("newsletter-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("ADMIN_BROADCAST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("NEWSLETTER")
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
    void modelContractsAreCovered() {
        NotificationDispatchEvent original = fixture();
        NotificationDispatchEvent same = fixture();
        assertThat(original)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isEqualTo(original)
                .isNotNull()
                .isNotEqualTo(new Object());

        assertAllNotEqual(original, changedFieldVariants());
        assertAllNotEqualBothWays(original, nullMismatchVariants());

        NotificationDispatchEvent child = new ChildEvent();
        child.setEventId(original.getEventId());
        child.setSourceService(original.getSourceService());
        child.setDispatchChannel(original.getDispatchChannel());
        child.setRecipientId(original.getRecipientId());
        child.setRecipientIds(original.getRecipientIds());
        child.setActorId(original.getActorId());
        child.setType(original.getType());
        child.setTitle(original.getTitle());
        child.setMessage(original.getMessage());
        child.setRelatedId(original.getRelatedId());
        child.setRelatedType(original.getRelatedType());
        child.setOccurredAt(original.getOccurredAt());
        assertThat(original).isNotEqualTo(child).isNotEqualTo(new NotificationDispatchEvent());

        assertThat(original.toString()).contains("newsletter-service");
        assertThat(new NotificationDispatchEvent()).isNotNull();
    }

    private static final class ChildEvent extends NotificationDispatchEvent {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }

    private NotificationDispatchEvent fixture() {
        return NotificationDispatchEvent.builder()
                .eventId("evt-1")
                .sourceService("newsletter-service")
                .dispatchChannel("IN_APP")
                .recipientId(10L)
                .recipientIds(List.of(10L, 11L))
                .actorId(1L)
                .type("ADMIN_BROADCAST")
                .title("title")
                .message("message")
                .relatedId(5L)
                .relatedType("NEWSLETTER")
                .occurredAt(Instant.parse("2025-01-03T00:00:00Z"))
                .build();
    }

    private List<NotificationDispatchEvent> changedFieldVariants() {
        NotificationDispatchEvent type = fixture();
        type.setType("NEW_POST");
        NotificationDispatchEvent actor = fixture();
        actor.setActorId(7L);
        NotificationDispatchEvent relatedId = fixture();
        relatedId.setRelatedId(99L);
        NotificationDispatchEvent source = fixture();
        source.setSourceService("other");
        NotificationDispatchEvent eventId = fixture();
        eventId.setEventId("evt-2");
        NotificationDispatchEvent relatedType = fixture();
        relatedType.setRelatedType("POST");
        return List.of(type, actor, relatedId, source, eventId, relatedType);
    }

    private List<NotificationDispatchEvent> nullMismatchVariants() {
        NotificationDispatchEvent eventId = fixture();
        eventId.setEventId(null);
        NotificationDispatchEvent sourceService = fixture();
        sourceService.setSourceService(null);
        NotificationDispatchEvent channel = fixture();
        channel.setDispatchChannel(null);
        NotificationDispatchEvent recipientId = fixture();
        recipientId.setRecipientId(null);
        NotificationDispatchEvent recipientIds = fixture();
        recipientIds.setRecipientIds(null);
        NotificationDispatchEvent actor = fixture();
        actor.setActorId(null);
        NotificationDispatchEvent type = fixture();
        type.setType(null);
        NotificationDispatchEvent title = fixture();
        title.setTitle(null);
        NotificationDispatchEvent message = fixture();
        message.setMessage(null);
        NotificationDispatchEvent relatedId = fixture();
        relatedId.setRelatedId(null);
        NotificationDispatchEvent relatedType = fixture();
        relatedType.setRelatedType(null);
        NotificationDispatchEvent occurredAt = fixture();
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
