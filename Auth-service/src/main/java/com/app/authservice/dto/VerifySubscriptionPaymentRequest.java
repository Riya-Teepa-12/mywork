package com.app.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifySubscriptionPaymentRequest {

    @NotBlank
    private String providerOrderId;

    @NotBlank
    private String providerPaymentId;

    @NotBlank
    private String providerSignature;
}

