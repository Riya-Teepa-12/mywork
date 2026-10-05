package com.app.authservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.authservice.dto.CreateSubscriptionOrderRequest;
import com.app.authservice.dto.CreateSubscriptionOrderResponse;
import com.app.authservice.dto.SubscriptionEntitlementResponse;
import com.app.authservice.dto.SubscriptionPlansResponse;
import com.app.authservice.dto.SubscriptionSummaryResponse;
import com.app.authservice.dto.VerifySubscriptionPaymentRequest;
import com.app.authservice.service.SubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Value("${auth.internal-api-key:ghfyfr7t8hgv7yh}")
    private String configuredInternalApiKey;

    @GetMapping("/subscriptions/plans")
    public SubscriptionPlansResponse getPlans() {
        return subscriptionService.getPlans();
    }

    @GetMapping("/subscriptions/me")
    public SubscriptionEntitlementResponse getMyEntitlements(Authentication authentication) {
        return subscriptionService.getEntitlementsByEmail(authentication.getName());
    }

    @PostMapping("/subscriptions/order")
    public CreateSubscriptionOrderResponse createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateSubscriptionOrderRequest request) {
        return subscriptionService.createOrder(authentication.getName(), request);
    }

    @PostMapping("/subscriptions/verify")
    public SubscriptionEntitlementResponse verifyPayment(
            Authentication authentication,
            @Valid @RequestBody VerifySubscriptionPaymentRequest request) {
        return subscriptionService.verifyOrder(authentication.getName(), request);
    }

    @GetMapping("/subscriptions/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SubscriptionSummaryResponse> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    @GetMapping("/internal/entitlements/{userId}")
    public SubscriptionEntitlementResponse getInternalEntitlements(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedInternalApiKey) {
        requireInternalApiKey(providedInternalApiKey);
        return subscriptionService.getEntitlementsByUserId(userId);
    }

    private void requireInternalApiKey(String providedInternalApiKey) {
        if (providedInternalApiKey == null || providedInternalApiKey.isBlank()
                || !providedInternalApiKey.equals(configuredInternalApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
        }
    }
}

