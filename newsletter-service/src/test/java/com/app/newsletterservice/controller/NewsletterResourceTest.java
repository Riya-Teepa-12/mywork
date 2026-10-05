package com.app.newsletterservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.app.newsletterservice.dto.DispatchResponse;
import com.app.newsletterservice.dto.SendNewsletterRequest;
import com.app.newsletterservice.dto.SendPostNotificationRequest;
import com.app.newsletterservice.dto.SubscriberResponse;
import com.app.newsletterservice.dto.SubscribeRequest;
import com.app.newsletterservice.dto.UpdatePreferencesRequest;
import com.app.newsletterservice.entity.SubscriberStatus;
import com.app.newsletterservice.service.NewsletterService;

@ExtendWith(MockitoExtension.class)
class NewsletterResourceTest {

    @Mock
    private NewsletterService newsletterService;

    @Test
    void subscribeAndConfirmDelegateToService() {
        NewsletterResource resource = new NewsletterResource(newsletterService);
        SubscribeRequest request = new SubscribeRequest();
        request.setEmail("reader@example.com");
        when(newsletterService.subscribe(request)).thenReturn(SubscriberResponse.builder().email("reader@example.com").build());
        when(newsletterService.confirmSubscription("token")).thenReturn(SubscriberResponse.builder().email("reader@example.com").build());

        resource.subscribe(request);
        resource.confirm("token");

        verify(newsletterService).subscribe(request);
        verify(newsletterService).confirmSubscription("token");
    }

    @Test
    void authenticatedRoutesEnforceHeaders() {
        NewsletterResource resource = new NewsletterResource(newsletterService);

        assertThatThrownBy(() -> resource.getMySubscription(null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> resource.getAll("READER"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sendAndCountRoutesDelegateToService() {
        NewsletterResource resource = new NewsletterResource(newsletterService);
        SendNewsletterRequest newsletterRequest = new SendNewsletterRequest();
        newsletterRequest.setSubject("Update");
        newsletterRequest.setContent("Body");
        SendPostNotificationRequest postRequest = new SendPostNotificationRequest();
        postRequest.setPostId(1L);
        postRequest.setTitle("New post");
        postRequest.setSlug("new-post");
        UpdatePreferencesRequest updatePreferencesRequest = new UpdatePreferencesRequest();
        updatePreferencesRequest.setEmail("reader@example.com");
        when(newsletterService.sendNewsletter(newsletterRequest)).thenReturn(new DispatchResponse("Newsletter sent", 1));
        when(newsletterService.sendPostNotification(postRequest)).thenReturn(new DispatchResponse("Post notification sent", 1));
        when(newsletterService.updatePreferences(updatePreferencesRequest)).thenReturn(SubscriberResponse.builder().email("reader@example.com").build());
        when(newsletterService.sendWelcomeEmail("reader@example.com")).thenReturn(new DispatchResponse("Welcome email sent", 1));
        when(newsletterService.getSubscriberCount(SubscriberStatus.ACTIVE)).thenReturn(3L);
        when(newsletterService.getAllSubscribers()).thenReturn(List.of());

        resource.sendNewsletter(newsletterRequest, "ADMIN");
        resource.sendPostNotification(postRequest, null);
        resource.updatePreferences(updatePreferencesRequest);
        resource.sendWelcome("reader@example.com", "ADMIN");
        resource.count(SubscriberStatus.ACTIVE, "ADMIN");
        resource.getAll("ADMIN");

        verify(newsletterService).sendNewsletter(newsletterRequest);
        verify(newsletterService).sendPostNotification(postRequest);
        verify(newsletterService).updatePreferences(updatePreferencesRequest);
        verify(newsletterService).sendWelcomeEmail("reader@example.com");
    }

//    @Test
//    void mySubscriptionHandlesNotFoundAndForbiddenBranches() {
//        NewsletterResource resource = new NewsletterResource(newsletterService);
//        when(newsletterService.getSubscriberByEmail("reader@example.com"))
//                .thenThrow(new IllegalArgumentException("Subscriber not found"));
//
//        org.springframework.http.ResponseEntity<SubscriberResponse> response =
//                resource.getMySubscription("reader@example.com");
//        SendPostNotificationRequest request = new SendPostNotificationRequest();
//
//        org.assertj.core.api.Assertions.assertThat(response.getBody()).isNull();
//        assertThatThrownBy(() -> resource.sendPostNotification(request, "AUTHOR"))
//                .isInstanceOf(ResponseStatusException.class);
//    }

    @Test
    void mySubscriptionHandlesNotFoundAndForbiddenBranches() {
        NewsletterResource resource = new NewsletterResource(newsletterService);

        when(newsletterService.getSubscriberByEmail("reader@example.com"))
                .thenThrow(new IllegalArgumentException("Subscriber not found"));

        ResponseEntity<Map<String, Object>> response =
                resource.getMySubscription("reader@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("subscribed", false);

        SendPostNotificationRequest request = new SendPostNotificationRequest();
        assertThatThrownBy(() -> resource.sendPostNotification(request, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }
}


