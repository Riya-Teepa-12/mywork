package com.app.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditLogRequest {
    @NotBlank
    private String action;

    private String targetType;
    private Long targetId;
    private String details;
}
