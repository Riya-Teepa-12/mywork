package com.app.authservice.dto;

import com.app.authservice.entity.SubscriptionPlanType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionPlanInfoResponse {

    private SubscriptionPlanType planType;
    private String label;
    private String description;
    private Long amountPaise;
    private String currency;
    private Integer durationDays;
    private boolean includesNewsletter;
}

