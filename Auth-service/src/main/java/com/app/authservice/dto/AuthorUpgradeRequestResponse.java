package com.app.authservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.app.authservice.entity.AuthorUpgradeStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorUpgradeRequestResponse {
    private Long requestId;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String bio;
    private String motivation;
    private List<String> expertiseCategories;
    private List<String> writingSampleUrls;
    private AuthorUpgradeStatus status;
    private String decisionReason;
    private Long reviewedByUserId;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
