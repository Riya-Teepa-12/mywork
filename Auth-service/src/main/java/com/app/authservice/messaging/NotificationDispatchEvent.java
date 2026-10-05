package com.app.authservice.messaging;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDispatchEvent {
    private String eventId;
    private String sourceService;
    private String dispatchChannel;
    private Long recipientId;
    private List<Long> recipientIds;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private Instant occurredAt;
}
