package com.app.newsletterservice.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.app.newsletterservice.dto.DispatchResponse;
import com.app.newsletterservice.dto.SendNewsletterRequest;
import com.app.newsletterservice.dto.SendPostNotificationRequest;
import com.app.newsletterservice.dto.SubscriberResponse;
import com.app.newsletterservice.dto.SubscribeRequest;
import com.app.newsletterservice.dto.UpdatePreferencesRequest;
import com.app.newsletterservice.entity.Subscriber;
import com.app.newsletterservice.entity.SubscriberStatus;
import com.app.newsletterservice.messaging.NotificationDispatchEvent;
import com.app.newsletterservice.repository.SubscriberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsletterServiceImpl implements NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterServiceImpl.class);

    private final SubscriberRepository subscriberRepository;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${inkwell.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${inkwell.notification.kafka.topic:notification.dispatch.v1}")
    private String notificationKafkaTopic;

    @Value("${spring.application.name:newsletter-service}")
    private String applicationName;

    @Value("${inkwell.newsletter.public-base-url:http://localhost:8080/newsletter}")
    private String newsletterPublicBaseUrl;

    @Value("${inkwell.auth-service-url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${inkwell.internal-api-key:ghfyfr7t8hgv7yh}")
    private String internalApiKey;

    @Override
    @Transactional
    public SubscriberResponse subscribe(SubscribeRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Login and active newsletter subscription are required");
        }
        EntitlementResponse entitlement = fetchEntitlements(request.getUserId());
        if (entitlement == null || (!entitlement.isAdmin() && !entitlement.isNewsletterEntitled())) {
            throw new IllegalArgumentException("Buy newsletter subscription to continue");
        }

        String email = normalizeEmail(request.getEmail());
        Subscriber subscriber = subscriberRepository.findByEmail(email).orElse(null);
        if (subscriber == null) {
            subscriber = Subscriber.builder()
                    .email(email)
                    .userId(request.getUserId())
                    .fullName(request.getFullName())
                    .preferences(joinPreferences(request.getPreferences()))
                    .status(SubscriberStatus.PENDING)
                    .token(newToken())
                    .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                    .subscribedAt(LocalDateTime.now())
                    .build();
        } else {
            subscriber.setUserId(request.getUserId());
            subscriber.setFullName(request.getFullName());
            subscriber.setPreferences(joinPreferences(request.getPreferences()));
            subscriber.setStatus(SubscriberStatus.PENDING);
            subscriber.setToken(newToken());
            subscriber.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
            subscriber.setSubscribedAt(LocalDateTime.now());
            subscriber.setUnsubscribedAt(null);
        }
        subscriber = subscriberRepository.save(subscriber);
        sendConfirmationEmail(subscriber);
        sendInAppNotification(
                subscriber,
                "Newsletter subscription pending confirmation",
                "Please check your email and confirm subscription using the verification link.");
        return toResponse(subscriber);
    }

    @Override
    @Transactional
    public DispatchResponse unsubscribe(String token) {
        Subscriber subscriber = findByToken(token);
        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);
        sendPlainEmail(
                subscriber.getEmail(),
                "You are unsubscribed from InkWell",
                "Hi " + displayName(subscriber) + ",\n\nYour newsletter subscription is now unsubscribed.");
        sendInAppNotification(
                subscriber,
                "Newsletter unsubscribed",
                "You have been unsubscribed from the InkWell newsletter.");
        return new DispatchResponse("Unsubscribed successfully", 1);
    }

    @Override
    @Transactional
    public SubscriberResponse confirmSubscription(String token) {
        Subscriber subscriber = findByToken(token);
        if (subscriber.getTokenExpiresAt() == null || subscriber.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Confirmation token expired");
        }
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setTokenExpiresAt(LocalDateTime.now().plusYears(2));
        subscriber = subscriberRepository.save(subscriber);
        sendPlainEmail(
                subscriber.getEmail(),
                "InkWell subscription confirmed",
                "Hi " + displayName(subscriber) + ",\n\nYour newsletter subscription is active now.");
        sendInAppNotification(
                subscriber,
                "Newsletter subscription confirmed",
                "Your newsletter subscription is active now.");
        return toResponse(subscriber);
    }

    @Override
    public SubscriberResponse getSubscriberByEmail(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
        return toResponse(subscriber);
    }

    @Override
    public List<SubscriberResponse> getAllSubscribers() {
        return subscriberRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public DispatchResponse sendNewsletter(SendNewsletterRequest request) {
        SubscriberStatus statusFilter = request.getStatusFilter() == null ? SubscriberStatus.ACTIVE : request.getStatusFilter();
        List<Subscriber> recipients = subscriberRepository.findByStatus(statusFilter);
        if (request.getPreferenceFilter() != null && !request.getPreferenceFilter().isEmpty()) {
            Set<String> filters = request.getPreferenceFilter().stream()
                    .map(value -> value.toLowerCase(Locale.ROOT).trim())
                    .collect(Collectors.toSet());
            recipients = recipients.stream()
                    .filter(subscriber -> !disjoint(parsePreferences(subscriber.getPreferences()), filters))
                    .toList();
        }
        recipients = filterEntitledRecipients(recipients);
        long sent = 0;
        for (Subscriber subscriber : recipients) {
            String unsubscribeUrl = buildUnsubscribeUrl(subscriber.getToken());
            sendPlainEmail(
                    subscriber.getEmail(),
                    request.getSubject(),
                    "Hi " + displayName(subscriber) + ",\n\n" + request.getContent()
                            + "\n\nUnsubscribe in one click:\n" + unsubscribeUrl);
            sendInAppNotification(
                    subscriber,
                    "ADMIN_BROADCAST",
                    "Newsletter: " + request.getSubject(),
                    request.getContent(),
                    subscriber.getSubscriberId(),
                    "NEWSLETTER");
            sent++;
        }
        return new DispatchResponse("Newsletter sent", sent);
    }

    @Override
    public DispatchResponse sendPostNotification(SendPostNotificationRequest request) {
        List<Subscriber> recipients = filterEntitledRecipients(
                subscriberRepository.findByStatus(SubscriberStatus.ACTIVE));
        String postLink = trimTrailingSlash(frontendUrl) + "/post/" + encode(request.getSlug());
        long sent = 0;
        for (Subscriber subscriber : recipients) {
            String unsubscribeUrl = buildUnsubscribeUrl(subscriber.getToken());
            String body = "Hi " + displayName(subscriber) + ",\n\nNew post published: " + request.getTitle()
                    + "\n\n" + nullSafe(request.getExcerpt())
                    + "\n\nRead now: " + postLink
                    + "\n\nUnsubscribe in one click:\n" + unsubscribeUrl;
            sendPlainEmail(subscriber.getEmail(), "New post: " + request.getTitle(), body);
            String inAppMessage = nullSafe(request.getExcerpt()).isBlank()
                    ? "A new post is now live. Tap to read it."
                    : request.getExcerpt();
            sendInAppNotification(
                    subscriber,
                    "NEW_POST",
                    "New post published: " + request.getTitle(),
                    inAppMessage + "\nRead now: " + postLink,
                    request.getPostId(),
                    "POST");
            sent++;
        }
        return new DispatchResponse("Post notification sent", sent);
    }

    @Override
    @Transactional
    public SubscriberResponse updatePreferences(UpdatePreferencesRequest request) {
        Subscriber subscriber = subscriberRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
        subscriber.setPreferences(joinPreferences(request.getPreferences()));
        subscriber = subscriberRepository.save(subscriber);
        return toResponse(subscriber);
    }

    @Override
    public long getSubscriberCount(SubscriberStatus status) {
        if (status == null) {
            return subscriberRepository.count();
        }
        return subscriberRepository.countByStatus(status);
    }

    @Override
    public DispatchResponse sendWelcomeEmail(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
        String unsubscribeUrl = buildUnsubscribeUrl(subscriber.getToken());
        sendPlainEmail(
                subscriber.getEmail(),
                "Welcome to InkWell Newsletter",
                "Hi " + displayName(subscriber) + ",\n\nWelcome to InkWell newsletter."
                        + "\n\nUnsubscribe in one click:\n" + unsubscribeUrl);
        return new DispatchResponse("Welcome email sent", 1);
    }

    private Subscriber findByToken(String token) {
        return subscriberRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
    }

    private String joinPreferences(List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "";
        }
        return preferences.stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private Set<String> parsePreferences(String preferences) {
        if (preferences == null || preferences.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(preferences.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean disjoint(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        return UUID.randomUUID().toString();
    }

    private void sendConfirmationEmail(Subscriber subscriber) {
        String confirmUrl = trimTrailingSlash(newsletterPublicBaseUrl) + "/confirm?token=" + encode(subscriber.getToken());
        String unsubscribeUrl = buildUnsubscribeUrl(subscriber.getToken());
        String body = "Hi " + displayName(subscriber) + ",\n\n"
                + "Please confirm your newsletter subscription:\n" + confirmUrl
                + "\n\nIf this was not you, ignore this mail.\n\nUnsubscribe link:\n" + unsubscribeUrl;
        sendPlainEmail(subscriber.getEmail(), "Confirm your InkWell subscription", body);
    }

    private String buildUnsubscribeUrl(String token) {
        return trimTrailingSlash(newsletterPublicBaseUrl) + "/unsubscribe?token=" + encode(token);
    }

    private void sendPlainEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendInAppNotification(Subscriber subscriber, String title, String message) {
        sendInAppNotification(
                subscriber,
                "ADMIN_BROADCAST",
                title,
                message,
                subscriber.getSubscriberId(),
                "NEWSLETTER");
    }

    private void sendInAppNotification(
            Subscriber subscriber,
            String type,
            String title,
            String message,
            Long relatedId,
            String relatedType) {
        if (subscriber.getUserId() == null) {
            return;
        }
        NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("IN_APP")
                .recipientId(subscriber.getUserId())
                .actorId(subscriber.getUserId())
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(event);
    }

    private void publishNotificationEvent(NotificationDispatchEvent event) {
        if (notificationKafkaTemplateProvider == null) {
            return;
        }
        KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate =
                notificationKafkaTemplateProvider.getIfAvailable();
        if (notificationKafkaTemplate == null) {
            return;
        }
        String key = event.getRecipientId() == null ? UUID.randomUUID().toString() : String.valueOf(event.getRecipientId());
        notificationKafkaTemplate.send(notificationKafkaTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed for newsletter notification event {}", event.getEventId(), ex);
                    }
                });
    }

    private String displayName(Subscriber subscriber) {
        if (subscriber.getFullName() != null && !subscriber.getFullName().isBlank()) {
            return subscriber.getFullName();
        }
        return subscriber.getEmail();
    }

    private String nullSafe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private List<Subscriber> filterEntitledRecipients(List<Subscriber> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        Map<Long, EntitlementResponse> cache = new HashMap<>();
        return recipients.stream()
                .filter(subscriber -> subscriber.getUserId() != null)
                .filter(subscriber -> {
                    Long userId = subscriber.getUserId();
                    EntitlementResponse entitlement = cache.computeIfAbsent(userId, this::fetchEntitlements);
                    return entitlement != null && (entitlement.isAdmin() || entitlement.isNewsletterEntitled());
                })
                .toList();
    }

    private EntitlementResponse fetchEntitlements(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<EntitlementResponse> response = restTemplate.exchange(
                    trimTrailingSlash(authServiceUrl) + "/auth/internal/entitlements/" + userId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    EntitlementResponse.class);
            return response.getBody();
        } catch (Exception ignored) {
            return null;
        }
    }

    private SubscriberResponse toResponse(Subscriber subscriber) {
        return SubscriberResponse.builder()
                .subscriberId(subscriber.getSubscriberId())
                .email(subscriber.getEmail())
                .userId(subscriber.getUserId())
                .fullName(subscriber.getFullName())
                .status(subscriber.getStatus())
                .subscribedAt(subscriber.getSubscribedAt())
                .unsubscribedAt(subscriber.getUnsubscribedAt())
                .token(subscriber.getToken())
                .tokenExpiresAt(subscriber.getTokenExpiresAt())
                .preferences(parsePreferences(subscriber.getPreferences()).stream().toList())
                .build();
    }

    private static class EntitlementResponse {
        private boolean admin;
        private boolean newsletterEntitled;

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public boolean isNewsletterEntitled() {
            return newsletterEntitled;
        }

        public void setNewsletterEntitled(boolean newsletterEntitled) {
            this.newsletterEntitled = newsletterEntitled;
        }
    }
}
