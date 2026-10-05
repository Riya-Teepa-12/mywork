package com.app.notificationservice.service;

import java.util.List;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.DispatchResponse;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;

public interface NotificationService {
    NotificationResponse send(NotificationRequest request);
    DispatchResponse sendBulk(BulkNotificationRequest request);
    NotificationResponse markAsRead(Long notificationId);
    DispatchResponse markAllRead(Long recipientId);
    DispatchResponse deleteRead(Long recipientId);
    List<NotificationResponse> getByRecipient(Long recipientId);
    long getUnreadCount(Long recipientId);
    DispatchResponse deleteNotification(Long notificationId);
    DispatchResponse sendEmail(NotificationRequest request);
    List<NotificationResponse> getAll();
}
