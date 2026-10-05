package com.app.notificationservice.dto;

import java.util.List;

import com.app.notificationservice.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkNotificationRequest {
    @NotEmpty
    private List<Long> recipientIds;
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
