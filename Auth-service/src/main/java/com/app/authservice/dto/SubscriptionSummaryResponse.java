package com.app.authservice.dto;

import java.time.LocalDateTime;

import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionSummaryResponse {

    private Long subscriptionId;
    private Long userId;
    private SubscriptionPlanType planType;
    private SubscriptionStatus status;
    private Long amountPaise;
    private String currency;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String providerOrderId;
    private String providerPaymentId;
    private LocalDateTime createdAt;
}

