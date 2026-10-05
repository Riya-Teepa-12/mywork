package com.app.newsletterservice.service;

import java.util.List;

import com.app.newsletterservice.dto.DispatchResponse;
import com.app.newsletterservice.dto.SendNewsletterRequest;
import com.app.newsletterservice.dto.SendPostNotificationRequest;
import com.app.newsletterservice.dto.SubscriberResponse;
import com.app.newsletterservice.dto.SubscribeRequest;
import com.app.newsletterservice.dto.UpdatePreferencesRequest;
import com.app.newsletterservice.entity.SubscriberStatus;

public interface NewsletterService {
    SubscriberResponse subscribe(SubscribeRequest request);
    DispatchResponse unsubscribe(String token);
    SubscriberResponse confirmSubscription(String token);
    SubscriberResponse getSubscriberByEmail(String email);
    List<SubscriberResponse> getAllSubscribers();
    DispatchResponse sendNewsletter(SendNewsletterRequest request);
    DispatchResponse sendPostNotification(SendPostNotificationRequest request);
    SubscriberResponse updatePreferences(UpdatePreferencesRequest request);
    long getSubscriberCount(SubscriberStatus status);
    DispatchResponse sendWelcomeEmail(String email);
}
