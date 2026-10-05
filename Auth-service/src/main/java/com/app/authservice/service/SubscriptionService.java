package com.app.authservice.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.app.authservice.dto.CreateSubscriptionOrderRequest;
import com.app.authservice.dto.CreateSubscriptionOrderResponse;
import com.app.authservice.dto.SubscriptionEntitlementResponse;
import com.app.authservice.dto.SubscriptionPlanInfoResponse;
import com.app.authservice.dto.SubscriptionPlansResponse;
import com.app.authservice.dto.SubscriptionSummaryResponse;
import com.app.authservice.dto.VerifySubscriptionPaymentRequest;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${inkwell.billing.enabled:true}")
    private boolean billingEnabled;

    @Value("${inkwell.billing.free-author-post-limit:5}")
    private int freeAuthorPostLimit;

    @Value("${inkwell.billing.currency:INR}")
    private String billingCurrency;

    @Value("${inkwell.billing.subscription-duration-days:30}")
    private int subscriptionDurationDays;

    @Value("${inkwell.billing.author-posts.amount-paise:49900}")
    private long authorPostsAmountPaise;

    @Value("${inkwell.billing.newsletter.amount-paise:9900}")
    private long newsletterAmountPaise;

    @Value("${inkwell.billing.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${inkwell.billing.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${inkwell.post-service-url:http://localhost:8082}")
    private String postServiceUrl;

    public SubscriptionPlansResponse getPlans() {
        return SubscriptionPlansResponse.builder()
                .billingEnabled(billingEnabled)
                .freeAuthorPostLimit(freeAuthorPostLimit)
                .plans(List.of(
                        SubscriptionPlanInfoResponse.builder()
                                .planType(SubscriptionPlanType.AUTHOR_POSTS)
                                .label("Author Post Subscription")
                                .description("Required after free post limit. Includes newsletter entitlement by default.")
                                .amountPaise(authorPostsAmountPaise)
                                .currency(normalizedCurrency())
                                .durationDays(subscriptionDurationDays)
                                .includesNewsletter(true)
                                .build(),
                        SubscriptionPlanInfoResponse.builder()
                                .planType(SubscriptionPlanType.NEWSLETTER)
                                .label("Newsletter Subscription")
                                .description("Required to receive newsletter and new-post email campaigns.")
                                .amountPaise(newsletterAmountPaise)
                                .currency(normalizedCurrency())
                                .durationDays(subscriptionDurationDays)
                                .includesNewsletter(false)
                                .build()))
                .build();
    }

    public SubscriptionEntitlementResponse getEntitlementsByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildEntitlements(user);
    }

    public SubscriptionEntitlementResponse getEntitlementsByUserId(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildEntitlements(user);
    }

    @Transactional
    public CreateSubscriptionOrderResponse createOrder(String email, CreateSubscriptionOrderRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin does not require subscription purchase");
        }
        SubscriptionPlanType planType = request.getPlanType();
        if (planType == SubscriptionPlanType.AUTHOR_POSTS && user.getRole() != Role.AUTHOR) {
            throw new IllegalArgumentException("Author post subscription is available only for authors");
        }
        if (!billingEnabled) {
            throw new IllegalStateException("Billing is currently disabled");
        }
        ensureRazorpayConfigured();

        long amountPaise = amountForPlan(planType);
        String receipt = "inkwell_" + planType.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0,12).replace("-", "");
        Map<String, Object> providerOrder = createRazorpayOrder(amountPaise, normalizedCurrency(), receipt);
        String providerOrderId = stringValue(providerOrder.get("id"));
        if (providerOrderId == null || providerOrderId.isBlank()) {
            throw new IllegalStateException("Unable to create payment order");
        }

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .userId(user.getUserId())
                .planType(planType)
                .status(PaymentOrderStatus.CREATED)
                .amountPaise(amountPaise)
                .currency(normalizedCurrency())
                .providerOrderId(providerOrderId)
                .receipt(receipt)
                .build();
        paymentOrderRepository.save(paymentOrder);

        return CreateSubscriptionOrderResponse.builder()
                .planType(planType)
                .keyId(razorpayKeyId)
                .providerOrderId(providerOrderId)
                .amountPaise(amountPaise)
                .currency(normalizedCurrency())
                .displayName("InkWell")
                .description(planType == SubscriptionPlanType.AUTHOR_POSTS
                        ? "Author Post Subscription (includes newsletter)"
                        : "Newsletter Subscription")
                .prefillName(user.getFullName())
                .prefillEmail(user.getEmail())
                .build();
    }

    @Transactional
    public SubscriptionEntitlementResponse verifyOrder(String email, VerifySubscriptionPaymentRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        PaymentOrder order = paymentOrderRepository.findByProviderOrderId(request.getProviderOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        if (!order.getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Payment order does not belong to current user");
        }
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return buildEntitlements(user);
        }

        boolean validSignature = verifyRazorpaySignature(
                request.getProviderOrderId(),
                request.getProviderPaymentId(),
                request.getProviderSignature());
        if (!validSignature) {
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setProviderPaymentId(request.getProviderPaymentId());
            order.setProviderSignature(request.getProviderSignature());
            paymentOrderRepository.save(order);
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        order.setStatus(PaymentOrderStatus.PAID);
        order.setProviderPaymentId(request.getProviderPaymentId());
        order.setProviderSignature(request.getProviderSignature());
        paymentOrderRepository.save(order);

        activateSubscription(user.getUserId(), order.getPlanType(), order.getAmountPaise(), order.getCurrency(),
                order.getProviderOrderId(), order.getProviderPaymentId());
        if (order.getPlanType() == SubscriptionPlanType.AUTHOR_POSTS) {
            activateSubscription(user.getUserId(), SubscriptionPlanType.NEWSLETTER, 0L, order.getCurrency(),
                    order.getProviderOrderId(), order.getProviderPaymentId());
        }

        return buildEntitlements(user);
    }

    public List<SubscriptionSummaryResponse> getAllSubscriptions() {
        return userSubscriptionRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(entry -> SubscriptionSummaryResponse.builder()
                        .subscriptionId(entry.getSubscriptionId())
                        .userId(entry.getUserId())
                        .planType(entry.getPlanType())
                        .status(entry.getStatus())
                        .amountPaise(entry.getAmountPaise())
                        .currency(entry.getCurrency())
                        .startsAt(entry.getStartsAt())
                        .endsAt(entry.getEndsAt())
                        .providerOrderId(entry.getProviderOrderId())
                        .providerPaymentId(entry.getProviderPaymentId())
                        .createdAt(entry.getCreatedAt())
                        .build())
                .toList();
    }

    private SubscriptionEntitlementResponse buildEntitlements(User user) {
        boolean admin = user.getRole() == Role.ADMIN;
        boolean postSubscriptionActive = admin || hasActiveSubscription(user.getUserId(), SubscriptionPlanType.AUTHOR_POSTS);
        boolean newsletterSubscriptionActive = admin || hasActiveSubscription(user.getUserId(), SubscriptionPlanType.NEWSLETTER);
        boolean newsletterIncludedByAuthorPlan = postSubscriptionActive;
        boolean newsletterEntitled = admin || newsletterSubscriptionActive || newsletterIncludedByAuthorPlan;

        long usedPosts = user.getRole() == Role.AUTHOR ? fetchPostCount(user.getUserId()) : 0L;
        long remainingPosts = Math.max(0L, freeAuthorPostLimit - usedPosts);
        boolean canCreatePosts = admin || user.getRole() != Role.AUTHOR || postSubscriptionActive || remainingPosts > 0;

        return SubscriptionEntitlementResponse.builder()
                .userId(user.getUserId())
                .role(user.getRole())
                .admin(admin)
                .authorPostSubscriptionActive(postSubscriptionActive)
                .newsletterSubscriptionActive(newsletterSubscriptionActive)
                .newsletterIncludedByAuthorPlan(newsletterIncludedByAuthorPlan)
                .newsletterEntitled(newsletterEntitled)
                .canCreateAuthorPosts(canCreatePosts)
                .freeAuthorPostLimit(freeAuthorPostLimit)
                .freePostsUsed(usedPosts)
                .freePostsRemaining(remainingPosts)
                .build();
    }

    private boolean hasActiveSubscription(Long userId, SubscriptionPlanType planType) {
        return !userSubscriptionRepository.findActiveByUserIdAndPlanType(
                userId,
                planType,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now()).isEmpty();
    }

    private void activateSubscription(
            Long userId,
            SubscriptionPlanType planType,
            Long amountPaise,
            String currency,
            String providerOrderId,
            String providerPaymentId) {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription existing = userSubscriptionRepository
                .findTopByUserIdAndPlanTypeOrderBySubscriptionIdDesc(userId, planType)
                .orElse(null);

        LocalDateTime nextStart = now;
        if (existing != null && existing.getStatus() == SubscriptionStatus.ACTIVE && existing.getEndsAt() != null
                && existing.getEndsAt().isAfter(now)) {
            nextStart = existing.getEndsAt();
        }
        LocalDateTime nextEnd = nextStart.plusDays(Math.max(1, subscriptionDurationDays));

        if (existing == null) {
            existing = UserSubscription.builder()
                    .userId(userId)
                    .planType(planType)
                    .build();
        }
        existing.setStatus(SubscriptionStatus.ACTIVE);
        existing.setAmountPaise(amountPaise == null ? 0L : amountPaise);
        existing.setCurrency(currency == null || currency.isBlank() ? normalizedCurrency() : currency);
        existing.setPaymentProvider("RAZORPAY");
        existing.setProviderOrderId(providerOrderId);
        existing.setProviderPaymentId(providerPaymentId);
        existing.setStartsAt(nextStart);
        existing.setEndsAt(nextEnd);
        userSubscriptionRepository.save(existing);
    }

    private long amountForPlan(SubscriptionPlanType planType) {
        if (planType == SubscriptionPlanType.AUTHOR_POSTS) {
            return Math.max(100L, authorPostsAmountPaise);
        }
        return Math.max(100L, newsletterAmountPaise);
    }

    private Map<String, Object> createRazorpayOrder(long amountPaise, String currency, String receipt) {
        String endpoint = "https://api.razorpay.com/v1/orders";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8)));
        Map<String, Object> payload = Map.of(
                "amount", amountPaise,
                "currency", currency,
                "receipt", receipt);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || responseBody == null) {
            throw new IllegalStateException("Unable to create Razorpay order");
        }
        return responseBody;
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = bytesToHex(digest);
            return generated.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private long fetchPostCount(Long authorId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    trimTrailingSlash(postServiceUrl) + "/posts/count?authorId=" + authorId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return 0L;
            }
            Object value = responseBody.get("count");
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                return Long.parseLong(String.valueOf(value));
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new IllegalStateException("Razorpay is not configured");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizedCurrency() {
        return billingCurrency == null || billingCurrency.isBlank() ? "INR" : billingCurrency.trim().toUpperCase();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
