package com.app.newsletterservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.newsletterservice.dto.DispatchResponse;
import com.app.newsletterservice.dto.SendNewsletterRequest;
import com.app.newsletterservice.dto.SendPostNotificationRequest;
import com.app.newsletterservice.dto.SubscriberResponse;
import com.app.newsletterservice.dto.SubscribeRequest;
import com.app.newsletterservice.dto.UpdatePreferencesRequest;
import com.app.newsletterservice.entity.SubscriberStatus;
import com.app.newsletterservice.service.NewsletterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/newsletter")
@RequiredArgsConstructor
public class NewsletterResource {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public SubscriberResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        return newsletterService.subscribe(request);
    }

    @GetMapping("/confirm")
    public SubscriberResponse confirm(@RequestParam String token) {
        return newsletterService.confirmSubscription(token);
    }

//    @GetMapping("/me")
//    public ResponseEntity<SubscriberResponse> getMySubscription(
//            @RequestHeader(value = "X-User-Email", required = false) String actorEmail) {
//        requireAuthenticated(actorEmail);
//        try {
//            return ResponseEntity.ok(newsletterService.getSubscriberByEmail(actorEmail));
//        } catch (IllegalArgumentException ex) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMySubscription(
            @RequestHeader(value = "X-User-Email", required = false) String actorEmail) {
        requireAuthenticated(actorEmail);
        try {
            SubscriberResponse data = newsletterService.getSubscriberByEmail(actorEmail);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("subscribed", true, "data", data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("subscribed", false));
        }
    }

    @GetMapping("/unsubscribe")
    public DispatchResponse unsubscribe(@RequestParam String token) {
        return newsletterService.unsubscribe(token);
    }

    @GetMapping("/all")
    public List<SubscriberResponse> getAll(@RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return newsletterService.getAllSubscribers();
    }

    @PostMapping("/send-newsletter")
    public DispatchResponse sendNewsletter(
            @Valid @RequestBody SendNewsletterRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return newsletterService.sendNewsletter(request);
    }

    @PostMapping("/send-post-notification")
    public DispatchResponse sendPostNotification(
            @Valid @RequestBody SendPostNotificationRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdminIfHeaderPresent(actorRole);
        return newsletterService.sendPostNotification(request);
    }

    @PutMapping("/preferences")
    public SubscriberResponse updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return newsletterService.updatePreferences(request);
    }

    @PostMapping("/send-welcome")
    public DispatchResponse sendWelcome(
            @RequestParam String email,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return newsletterService.sendWelcomeEmail(email);
    }

    @GetMapping("/count")
    public Map<String, Long> count(
            @RequestParam(required = false) SubscriberStatus status,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return Map.of("count", newsletterService.getSubscriberCount(status));
    }

    private void requireAdmin(String actorRole) {
        if (!"ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
    }

    private void requireAdminIfHeaderPresent(String actorRole) {
        if (actorRole == null || actorRole.isBlank()) {
            return;
        }
        requireAdmin(actorRole);
    }

    private void requireAuthenticated(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
    }
}
