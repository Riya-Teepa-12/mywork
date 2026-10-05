package com.app.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.app.authservice.dto.CreateSubscriptionOrderRequest;
import com.app.authservice.dto.CreateSubscriptionOrderResponse;
import com.app.authservice.dto.SubscriptionEntitlementResponse;
import com.app.authservice.dto.SubscriptionPlansResponse;
import com.app.authservice.dto.VerifySubscriptionPaymentRequest;
import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.PaymentOrder;
import com.app.authservice.entity.PaymentOrderStatus;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;
import com.app.authservice.entity.User;
import com.app.authservice.entity.UserSubscription;
import com.app.authservice.repository.PaymentOrderRepository;
import com.app.authservice.repository.UserRepository;
import com.app.authservice.repository.UserSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "billingEnabled", false);
        ReflectionTestUtils.setField(subscriptionService, "freeAuthorPostLimit", 5);
        ReflectionTestUtils.setField(subscriptionService, "billingCurrency", "INR");
        ReflectionTestUtils.setField(subscriptionService, "subscriptionDurationDays", 30);
        ReflectionTestUtils.setField(subscriptionService, "authorPostsAmountPaise", 49900L);
        ReflectionTestUtils.setField(subscriptionService, "newsletterAmountPaise", 9900L);
        ReflectionTestUtils.setField(subscriptionService, "razorpayKeyId", "key-id");
        ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "secret");
        ReflectionTestUtils.setField(subscriptionService, "postServiceUrl", "http://post");
        ReflectionTestUtils.setField(subscriptionService, "restTemplate", restTemplate);
    }

    @Test
    void plansAndEntitlementsAreBuiltFromStoredUsers() {
        User reader = User.builder()
                .userId(1L)
                .username("reader")
                .email("reader@example.com")
                .passwordHash("hash")
                .fullName("Reader")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(reader));
        when(userSubscriptionRepository.findActiveByUserIdAndPlanType(
                anyLong(),
                any(SubscriptionPlanType.class),
                any(SubscriptionStatus.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        SubscriptionPlansResponse plans = subscriptionService.getPlans();
        SubscriptionEntitlementResponse entitlements = subscriptionService.getEntitlementsByEmail("reader@example.com");

        assertThat(plans.getPlans()).hasSize(2);
        assertThat(entitlements.isNewsletterEntitled()).isFalse();
        assertThat(entitlements.isCanCreateAuthorPosts()).isTrue();
    }

    @Test
    void createOrderRejectsAdminsAndVerifyOrderCanBeGuardedByOwnership() {
        User admin = User.builder()
                .userId(2L)
                .username("admin")
                .email("admin@example.com")
                .passwordHash("hash")
                .fullName("Admin")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(java.util.Optional.of(admin));

        CreateSubscriptionOrderRequest request = new CreateSubscriptionOrderRequest();
        request.setPlanType(SubscriptionPlanType.NEWSLETTER);
        assertThatThrownBy(() -> subscriptionService.createOrder("admin@example.com", request))
                .isInstanceOf(IllegalArgumentException.class);

        when(userRepository.findByEmail("author@example.com")).thenReturn(java.util.Optional.of(admin));
        VerifySubscriptionPaymentRequest verifyRequest = new VerifySubscriptionPaymentRequest();
        verifyRequest.setProviderOrderId("order-1");
        verifyRequest.setProviderPaymentId("payment-1");
        verifyRequest.setProviderSignature("signature");
        assertThatThrownBy(() -> subscriptionService.verifyOrder("author@example.com", verifyRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAllSubscriptionsMapsRepositoryRows() {
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId(1L)
                .userId(1L)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .status(SubscriptionStatus.ACTIVE)
                .amountPaise(9900L)
                .currency("INR")
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .providerOrderId("order-1")
                .providerPaymentId("payment-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(userSubscriptionRepository.findAll()).thenReturn(List.of(subscription));

        assertThat(subscriptionService.getAllSubscriptions()).hasSize(1);
    }

    @Test
    void createOrderCoversBillingDisabledAndSuccessfulProviderOrder() {
        User author = User.builder()
                .userId(9L)
                .username("author")
                .email("author@example.com")
                .role(Role.AUTHOR)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("author@example.com")).thenReturn(java.util.Optional.of(author));

        CreateSubscriptionOrderRequest request = new CreateSubscriptionOrderRequest();
        request.setPlanType(SubscriptionPlanType.AUTHOR_POSTS);

        assertThatThrownBy(() -> subscriptionService.createOrder("author@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        ReflectionTestUtils.setField(subscriptionService, "billingEnabled", true);
        when(restTemplate.postForEntity(any(String.class), any(), any(Class.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "order_123")));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSubscriptionOrderResponse response = subscriptionService.createOrder("author@example.com", request);

        assertThat(response.getProviderOrderId()).isEqualTo("order_123");
        assertThat(response.getPlanType()).isEqualTo(SubscriptionPlanType.AUTHOR_POSTS);
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void createOrderFailsWhenProviderBodyIsNull() {
        ReflectionTestUtils.setField(subscriptionService, "billingEnabled", true);
        User author = User.builder()
                .userId(19L)
                .username("author2")
                .email("author2@example.com")
                .role(Role.AUTHOR)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("author2@example.com")).thenReturn(java.util.Optional.of(author));
        when(restTemplate.postForEntity(any(String.class), any(), any(Class.class)))
                .thenReturn(ResponseEntity.ok().build());

        CreateSubscriptionOrderRequest request = new CreateSubscriptionOrderRequest();
        request.setPlanType(SubscriptionPlanType.AUTHOR_POSTS);

        assertThatThrownBy(() -> subscriptionService.createOrder("author2@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to create Razorpay order");
    }

    @Test
    void verifyOrderCoversOwnershipFailurePaidFastPathAndInvalidSignature() {
        User user = User.builder()
                .userId(11L)
                .email("u@example.com")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("u@example.com")).thenReturn(java.util.Optional.of(user));

        VerifySubscriptionPaymentRequest request = new VerifySubscriptionPaymentRequest();
        request.setProviderOrderId("order-1");
        request.setProviderPaymentId("payment-1");
        request.setProviderSignature("bad-signature");

        PaymentOrder wrongOwnerOrder = PaymentOrder.builder()
                .providerOrderId("order-1")
                .userId(99L)
                .status(PaymentOrderStatus.CREATED)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .build();
        when(paymentOrderRepository.findByProviderOrderId("order-1")).thenReturn(java.util.Optional.of(wrongOwnerOrder));
        assertThatThrownBy(() -> subscriptionService.verifyOrder("u@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");

        PaymentOrder paidOrder = PaymentOrder.builder()
                .providerOrderId("order-1")
                .userId(11L)
                .status(PaymentOrderStatus.PAID)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .build();
        when(paymentOrderRepository.findByProviderOrderId("order-1")).thenReturn(java.util.Optional.of(paidOrder));
        when(userSubscriptionRepository.findActiveByUserIdAndPlanType(
                anyLong(),
                any(SubscriptionPlanType.class),
                any(SubscriptionStatus.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of());
        assertThat(subscriptionService.verifyOrder("u@example.com", request).getUserId()).isEqualTo(11L);

        PaymentOrder createdOrder = PaymentOrder.builder()
                .providerOrderId("order-1")
                .userId(11L)
                .status(PaymentOrderStatus.CREATED)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .build();
        when(paymentOrderRepository.findByProviderOrderId("order-1")).thenReturn(java.util.Optional.of(createdOrder));

        assertThatThrownBy(() -> subscriptionService.verifyOrder("u@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature verification failed");
        assertThat(createdOrder.getStatus()).isEqualTo(PaymentOrderStatus.FAILED);
    }

    @Test
    void verifyOrderSuccessActivatesSubscriptionAndReturnsEntitlements() {
        ReflectionTestUtils.setField(subscriptionService, "billingEnabled", true);
        User user = User.builder()
                .userId(12L)
                .email("a@example.com")
                .role(Role.AUTHOR)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(java.util.Optional.of(user));

        VerifySubscriptionPaymentRequest request = new VerifySubscriptionPaymentRequest();
        request.setProviderOrderId("order-2");
        request.setProviderPaymentId("payment-2");
        request.setProviderSignature(validSignature("order-2", "payment-2", "secret"));

        PaymentOrder created = PaymentOrder.builder()
                .providerOrderId("order-2")
                .userId(12L)
                .status(PaymentOrderStatus.CREATED)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .amountPaise(9900L)
                .currency("INR")
                .build();
        when(paymentOrderRepository.findByProviderOrderId("order-2")).thenReturn(java.util.Optional.of(created));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSubscriptionRepository.findTopByUserIdAndPlanTypeOrderBySubscriptionIdDesc(12L, SubscriptionPlanType.NEWSLETTER))
                .thenReturn(java.util.Optional.empty());
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSubscriptionRepository.findActiveByUserIdAndPlanType(
                anyLong(),
                any(SubscriptionPlanType.class),
                any(SubscriptionStatus.class),
                any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    SubscriptionPlanType planType = invocation.getArgument(1);
                    if (planType == SubscriptionPlanType.NEWSLETTER) {
                        return List.of(UserSubscription.builder().subscriptionId(1L).build());
                    }
                    return List.of();
                });
        when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("count", 1)));

        SubscriptionEntitlementResponse response = subscriptionService.verifyOrder("a@example.com", request);

        assertThat(response.isNewsletterEntitled()).isTrue();
        assertThat(created.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
    }

    @Test
    void privateHelpersBehaveAsExpected() {
        ReflectionTestUtils.setField(subscriptionService, "billingCurrency", " ");
        assertThat((String) ReflectionTestUtils.invokeMethod(subscriptionService, "normalizedCurrency")).isEqualTo("INR");
        assertThat((String) ReflectionTestUtils.invokeMethod(subscriptionService, "trimTrailingSlash", "http://x/"))
                .isEqualTo("http://x");
        assertThat((Long) ReflectionTestUtils.invokeMethod(subscriptionService, "amountForPlan", SubscriptionPlanType.NEWSLETTER))
                .isGreaterThanOrEqualTo(100L);
        assertThat((String) ReflectionTestUtils.invokeMethod(subscriptionService, "stringValue", 123))
                .isEqualTo("123");
    }

    @Test
    void fetchPostCountParsesStringValueAndHandlesNullBody() {
        when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("count", "7")))
                .thenReturn(ResponseEntity.ok().build());

        Long parsed = (Long) ReflectionTestUtils.invokeMethod(subscriptionService, "fetchPostCount", 44L);
        Long fallback = (Long) ReflectionTestUtils.invokeMethod(subscriptionService, "fetchPostCount", 44L);

        assertThat(parsed).isEqualTo(7L);
        assertThat(fallback).isZero();
    }

    private String validSignature(String orderId, String paymentId, String secret) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}



