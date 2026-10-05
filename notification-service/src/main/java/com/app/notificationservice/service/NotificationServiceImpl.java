package com.app.notificationservice.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.DispatchResponse;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.entity.AppUser;
import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.repository.AppUserRepository;
import com.app.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final JavaMailSender mailSender;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${inkwell.notification.unread-cache-prefix:notification:unread}")
    private String unreadCachePrefix;

    @Value("${inkwell.notification.unread-cache-ttl-seconds:120}")
    private long unreadCacheTtlSeconds;

    @Override
    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = notificationRepository.save(Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build());
        evictUnreadCountCache(request.getRecipientId());
        return toResponse(notification);
    }

    @Override
    @Transactional
    public DispatchResponse sendBulk(BulkNotificationRequest request) {
        List<Notification> notifications = request.getRecipientIds().stream()
                .map(recipientId -> Notification.builder()
                        .recipientId(recipientId)
                        .actorId(request.getActorId())
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .relatedId(request.getRelatedId())
                        .relatedType(request.getRelatedType())
                        .isRead(false)
                .build())
                .toList();
        notificationRepository.saveAll(notifications);
        request.getRecipientIds().forEach(this::evictUnreadCountCache);
        return new DispatchResponse("Notifications sent", notifications.size());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = find(notificationId);
        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        evictUnreadCountCache(notification.getRecipientId());
        return toResponse(notification);
    }

    @Override
    @Transactional
    public DispatchResponse markAllRead(Long recipientId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
        unread.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unread);
        evictUnreadCountCache(recipientId);
        return new DispatchResponse("Marked all as read", unread.size());
    }

    @Override
    @Transactional
    public DispatchResponse deleteRead(Long recipientId) {
        long count = notificationRepository.findByRecipientIdAndIsRead(recipientId, true).size();
        notificationRepository.deleteByRecipientIdAndIsRead(recipientId, true);
        evictUnreadCountCache(recipientId);
        return new DispatchResponse("Deleted read notifications", count);
    }

    @Override
    public List<NotificationResponse> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        Long cached = getUnreadCountFromCache(recipientId);
        if (cached != null) {
            return cached;
        }
        long count = notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
        cacheUnreadCount(recipientId, count);
        return count;
    }

    @Override
    @Transactional
    public DispatchResponse deleteNotification(Long notificationId) {
        Notification notification = find(notificationId);
        notificationRepository.deleteByNotificationId(notificationId);
        evictUnreadCountCache(notification.getRecipientId());
        return new DispatchResponse("Notification deleted", 1);
    }

    @Override
    public DispatchResponse sendEmail(NotificationRequest request) {
        AppUser recipient = appUserRepository.findByUserId(request.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found"));
        if (!recipient.isActive()) {
            throw new IllegalArgumentException("Recipient account is inactive");
        }
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            throw new IllegalArgumentException("Recipient email is not set");
        }

        String name = recipient.getFullName() == null || recipient.getFullName().isBlank()
                ? recipient.getEmail()
                : recipient.getFullName();
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(name).append(",\n\n");
        body.append(request.getMessage());
        if (request.getRelatedType() != null && !request.getRelatedType().isBlank() && request.getRelatedId() != null) {
            body.append("\n\nRelated: ").append(request.getRelatedType()).append(" #").append(request.getRelatedId());
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipient.getEmail());
        message.setSubject(request.getTitle());
        message.setText(body.toString());
        mailSender.send(message);
        return new DispatchResponse("Email notification sent", 1);
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Notification find(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }

    private Long getUnreadCountFromCache(Long recipientId) {
        if (recipientId == null) {
            return null;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(unreadCacheKey(recipientId));
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.parseLong(value);
        } catch (Exception ex) {
            log.debug("Failed to read unread-count cache for user {}", recipientId, ex);
            return null;
        }
    }

    private void cacheUnreadCount(Long recipientId, long count) {
        if (recipientId == null) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    unreadCacheKey(recipientId),
                    String.valueOf(Math.max(0L, count)),
                    Duration.ofSeconds(Math.max(30L, unreadCacheTtlSeconds)));
        } catch (Exception ex) {
            log.debug("Failed to write unread-count cache for user {}", recipientId, ex);
        }
    }

    private void evictUnreadCountCache(Long recipientId) {
        if (recipientId == null) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(unreadCacheKey(recipientId));
        } catch (Exception ex) {
            log.debug("Failed to evict unread-count cache for user {}", recipientId, ex);
        }
    }

    private String unreadCacheKey(Long recipientId) {
        return unreadCachePrefix + ":" + recipientId;
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .read(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
