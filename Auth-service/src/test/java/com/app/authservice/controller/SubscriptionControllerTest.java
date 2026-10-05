package com.app.authservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.authservice.dto.CreateSubscriptionOrderRequest;
import com.app.authservice.dto.CreateSubscriptionOrderResponse;
import com.app.authservice.dto.SubscriptionEntitlementResponse;
import com.app.authservice.dto.SubscriptionPlansResponse;
import com.app.authservice.dto.VerifySubscriptionPaymentRequest;
import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void subscriptionEndpointsDelegateToService() {
        SubscriptionController controller = new SubscriptionController(subscriptionService);
        ReflectionTestUtils.setField(controller, "configuredInternalApiKey", "ghfyfr7t8hgv7yh");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@example.com");
        CreateSubscriptionOrderRequest orderRequest = new CreateSubscriptionOrderRequest();
        orderRequest.setPlanType(SubscriptionPlanType.NEWSLETTER);
        VerifySubscriptionPaymentRequest verifyRequest = new VerifySubscriptionPaymentRequest();
        verifyRequest.setProviderOrderId("order-1");
        verifyRequest.setProviderPaymentId("payment-1");
        verifyRequest.setProviderSignature("signature");

        when(subscriptionService.getPlans()).thenReturn(SubscriptionPlansResponse.builder().billingEnabled(true).build());
        when(subscriptionService.getEntitlementsByEmail("user@example.com")).thenReturn(SubscriptionEntitlementResponse.builder().userId(1L).build());
        when(subscriptionService.createOrder("user@example.com", orderRequest)).thenReturn(CreateSubscriptionOrderResponse.builder().planType(SubscriptionPlanType.NEWSLETTER).build());
        when(subscriptionService.verifyOrder("user@example.com", verifyRequest)).thenReturn(SubscriptionEntitlementResponse.builder().userId(1L).build());
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of());
        when(subscriptionService.getEntitlementsByUserId(10L)).thenReturn(SubscriptionEntitlementResponse.builder().userId(10L).build());

        assertThat(controller.getPlans().isBillingEnabled()).isTrue();
        assertThat(controller.getMyEntitlements(authentication).getUserId()).isEqualTo(1L);
        assertThat(controller.createOrder(authentication, orderRequest).getPlanType()).isEqualTo(SubscriptionPlanType.NEWSLETTER);
        assertThat(controller.verifyPayment(authentication, verifyRequest).getUserId()).isEqualTo(1L);
        assertThat(controller.getAllSubscriptions()).isEmpty();
        assertThat(controller.getInternalEntitlements(10L, "ghfyfr7t8hgv7yh").getUserId()).isEqualTo(10L);

        verify(subscriptionService).getPlans();
        verify(subscriptionService).getEntitlementsByEmail("user@example.com");
        verify(subscriptionService).createOrder("user@example.com", orderRequest);
        verify(subscriptionService).verifyOrder("user@example.com", verifyRequest);
        verify(subscriptionService).getAllSubscriptions();
        verify(subscriptionService).getEntitlementsByUserId(10L);
    }
}


