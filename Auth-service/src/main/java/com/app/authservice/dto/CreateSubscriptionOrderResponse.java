package com.app.authservice.dto;

import com.app.authservice.entity.SubscriptionPlanType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSubscriptionOrderResponse {

    private SubscriptionPlanType planType;
    private String keyId;
    private String providerOrderId;
    private Long amountPaise;
    private String currency;
    private String displayName;
    private String description;
    private String prefillName;
    private String prefillEmail;
}

