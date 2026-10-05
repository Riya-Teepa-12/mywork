package com.app.authservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponse {
    private Long auditLogId;
    private Long actorUserId;
    private String actorEmail;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;
}
