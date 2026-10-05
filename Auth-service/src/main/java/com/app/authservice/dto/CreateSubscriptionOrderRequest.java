package com.app.authservice.dto;

import com.app.authservice.entity.SubscriptionPlanType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubscriptionOrderRequest {

    @NotNull
    private SubscriptionPlanType planType;
}

