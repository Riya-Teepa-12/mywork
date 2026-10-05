package com.app.notificationservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientId(Long recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByRelatedId(Long relatedId);
    List<Notification> findAllByOrderByCreatedAtDesc();
    void deleteByNotificationId(Long notificationId);
    void deleteByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
}
