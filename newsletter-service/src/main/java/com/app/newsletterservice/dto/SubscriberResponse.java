package com.app.newsletterservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.app.newsletterservice.entity.SubscriberStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriberResponse {
    private Long subscriberId;
    private String email;
    private Long userId;
    private String fullName;
    private SubscriberStatus status;
    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;
    private String token;
    private LocalDateTime tokenExpiresAt;
    private List<String> preferences;
}
