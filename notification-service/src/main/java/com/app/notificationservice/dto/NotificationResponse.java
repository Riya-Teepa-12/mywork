package com.app.notificationservice.dto;

import java.time.LocalDateTime;

import com.app.notificationservice.entity.NotificationType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private Boolean read;
    private LocalDateTime createdAt;
}
