package com.app.notificationservice.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.DispatchResponse;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.entity.NotificationType;
import com.app.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationResourceTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void sendAndBulkDelegateToService() {
        NotificationResource resource = new NotificationResource(notificationService);
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(1L);
        request.setActorId(2L);
        request.setType(NotificationType.NEW_POST);
        request.setTitle("Title");
        request.setMessage("Message");
        BulkNotificationRequest bulk = new BulkNotificationRequest();
        bulk.setRecipientIds(List.of(1L));
        bulk.setActorId(2L);
        bulk.setType(NotificationType.NEW_POST);
        bulk.setTitle("Title");
        bulk.setMessage("Message");
        when(notificationService.send(request)).thenReturn(NotificationResponse.builder().notificationId(1L).build());
        when(notificationService.sendBulk(bulk)).thenReturn(new DispatchResponse("Notifications sent", 1));
        when(notificationService.sendEmail(request)).thenReturn(new DispatchResponse("Email notification sent", 1));
        when(notificationService.getByRecipient(1L)).thenReturn(List.of());
        when(notificationService.markAsRead(1L)).thenReturn(NotificationResponse.builder().notificationId(1L).build());
        when(notificationService.markAllRead(1L)).thenReturn(new DispatchResponse("Marked all as read", 1));
        when(notificationService.deleteRead(1L)).thenReturn(new DispatchResponse("Deleted read notifications", 1));
        when(notificationService.getUnreadCount(1L)).thenReturn(1L);
        when(notificationService.deleteNotification(1L)).thenReturn(new DispatchResponse("Notification deleted", 1));
        when(notificationService.getAll()).thenReturn(List.of());

        resource.send(request);
        resource.sendBulk(bulk);
        resource.sendEmail(request);
        resource.getByRecipient(1L);
        resource.markAsRead(1L);
        resource.markAllRead(1L);
        resource.deleteRead(1L);
        resource.unreadCount(1L);
        resource.deleteNotification(1L);
        resource.getAll();

        verify(notificationService).send(request);
        verify(notificationService).sendBulk(bulk);
        verify(notificationService).sendEmail(request);
    }
}


