package com.app.notificationservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.DispatchResponse;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationResource {

    private final NotificationService notificationService;

    @PostMapping
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return notificationService.send(request);
    }

    @PostMapping("/bulk")
    public DispatchResponse sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
        return notificationService.sendBulk(request);
    }

    @PostMapping("/email")
    public DispatchResponse sendEmail(@Valid @RequestBody NotificationRequest request) {
        return notificationService.sendEmail(request);
    }

    @GetMapping("/recipient/{recipientId}")
    public List<NotificationResponse> getByRecipient(@PathVariable Long recipientId) {
        return notificationService.getByRecipient(recipientId);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PutMapping("/read-all")
    public DispatchResponse markAllRead(@RequestParam Long recipientId) {
        return notificationService.markAllRead(recipientId);
    }

    @DeleteMapping("/read")
    public DispatchResponse deleteRead(@RequestParam Long recipientId) {
        return notificationService.deleteRead(recipientId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestParam Long recipientId) {
        return Map.of("count", notificationService.getUnreadCount(recipientId));
    }

    @DeleteMapping("/{notificationId}")
    public DispatchResponse deleteNotification(@PathVariable Long notificationId) {
        return notificationService.deleteNotification(notificationId);
    }

    @GetMapping("/all")
    public List<NotificationResponse> getAll() {
        return notificationService.getAll();
    }
}
