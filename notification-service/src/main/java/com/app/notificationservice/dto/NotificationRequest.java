package com.app.notificationservice.dto;

import com.app.notificationservice.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {
    @NotNull
    private Long recipientId;
    @NotNull
    private Long actorId;
    @NotNull
    private NotificationType type;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private Long relatedId;
    private String relatedType;
}
