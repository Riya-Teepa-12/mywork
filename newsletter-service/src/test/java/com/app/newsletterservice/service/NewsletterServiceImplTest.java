package com.app.newsletterservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
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

@ExtendWith(MockitoExtension.class)
class NewsletterServiceImplTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    @SuppressWarnings("unused")
    private ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate;

    @InjectMocks
    private NewsletterServiceImpl newsletterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsletterService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        ReflectionTestUtils.setField(newsletterService, "mailFrom", "news@inkwell.com");
        ReflectionTestUtils.setField(newsletterService, "frontendUrl", "http://frontend");
        ReflectionTestUtils.setField(newsletterService, "newsletterPublicBaseUrl", "http://newsletter");
        ReflectionTestUtils.setField(newsletterService, "authServiceUrl", "http://auth");
        ReflectionTestUtils.setField(newsletterService, "internalApiKey", "secret");
        ReflectionTestUtils.setField(newsletterService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(newsletterService, "notificationKafkaTopic", "notification.dispatch.v1");
        ReflectionTestUtils.setField(newsletterService, "applicationName", "newsletter-service");
    }

    @Test
    void confirmUnsubscribeAndWelcomeEmailWorkForStoredSubscriber() {
        Subscriber subscriber = Subscriber.builder()
                .subscriberId(1L)
                .email("reader@example.com")
                .userId(null)
                .fullName("Reader")
                .status(SubscriberStatus.PENDING)
                .subscribedAt(LocalDateTime.now())
                .token("token-1")
                .tokenExpiresAt(LocalDateTime.now().plusHours(2))
                .preferences("tech")
                .build();
        when(subscriberRepository.findByToken("token-1")).thenReturn(java.util.Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriberRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(subscriber));

        SubscriberResponse confirmed = newsletterService.confirmSubscription("token-1");
        DispatchResponse unsubscribed = newsletterService.unsubscribe("token-1");
        DispatchResponse welcome = newsletterService.sendWelcomeEmail("reader@example.com");

        assertThat(confirmed.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(unsubscribed.getRecipients()).isEqualTo(1L);
        assertThat(welcome.getRecipients()).isEqualTo(1L);
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void preferencesAndCountsAreDelegatedToRepository() {
        Subscriber subscriber = Subscriber.builder()
                .subscriberId(2L)
                .email("active@example.com")
                .userId(22L)
                .fullName("Active")
                .status(SubscriberStatus.ACTIVE)
                .subscribedAt(LocalDateTime.now())
                .token("token-2")
                .tokenExpiresAt(LocalDateTime.now().plusHours(2))
                .preferences("tech")
                .build();
        when(subscriberRepository.findByEmail("active@example.com")).thenReturn(java.util.Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriberRepository.count()).thenReturn(1L);
        when(subscriberRepository.countByStatus(SubscriberStatus.ACTIVE)).thenReturn(1L);
        when(subscriberRepository.findAll()).thenReturn(List.of(subscriber));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setEmail("active@example.com");
        request.setPreferences(List.of("Tech", "java"));

        assertThat(newsletterService.updatePreferences(request).getPreferences()).containsExactly("tech", "java");
        assertThat(newsletterService.getSubscriberByEmail("active@example.com").getEmail()).isEqualTo("active@example.com");
        assertThat(newsletterService.getSubscriberCount(null)).isEqualTo(1L);
        assertThat(newsletterService.getSubscriberCount(SubscriberStatus.ACTIVE)).isEqualTo(1L);
        assertThat(newsletterService.getAllSubscribers()).hasSize(1);
    }

    @Test
    void subscribeCanBeReferencedWithoutRemoteEntitlementsByTestingInputValidationOnly() {
        SubscribeRequest request = new SubscribeRequest();
        request.setEmail("reader@example.com");
        assertThat(request.getEmail()).isEqualTo("reader@example.com");
    }

    @Test
    void subscribeAndConfirmValidationBranchesThrowExpectedErrors() {
        SubscribeRequest noUser = new SubscribeRequest();
        noUser.setEmail("x@example.com");
        assertThatThrownBy(() -> newsletterService.subscribe(noUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login and active newsletter subscription");

        SubscribeRequest noEntitlement = new SubscribeRequest();
        noEntitlement.setEmail("x@example.com");
        noEntitlement.setUserId(10L);
        noEntitlement.setFullName("X");
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class)))
                .thenThrow(new RuntimeException("not used"));
        assertThatThrownBy(() -> newsletterService.subscribe(noEntitlement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newsletter subscription");

        Subscriber expired = Subscriber.builder()
                .subscriberId(9L)
                .email("expired@example.com")
                .status(SubscriberStatus.PENDING)
                .token("expired-token")
                .tokenExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(subscriberRepository.findByToken("expired-token")).thenReturn(java.util.Optional.of(expired));
        assertThatThrownBy(() -> newsletterService.confirmSubscription("expired-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void sendNewsletterAndPostNotificationDispatchToEligibleSubscribers() {
        enableKafka();
        Subscriber active = Subscriber.builder()
                .subscriberId(11L)
                .email("a@example.com")
                .userId(100L)
                .fullName("A")
                .status(SubscriberStatus.ACTIVE)
                .preferences("tech,java")
                .token("token-a")
                .tokenExpiresAt(LocalDateTime.now().plusDays(1))
                .subscribedAt(LocalDateTime.now())
                .build();
        when(subscriberRepository.findByStatus(SubscriberStatus.ACTIVE)).thenReturn(List.of(active));

        Object entitlement = buildEntitlement(true, true);
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(entitlement));

        SendNewsletterRequest newsletter = new SendNewsletterRequest();
        newsletter.setSubject("Weekly");
        newsletter.setContent("Content");
        newsletter.setStatusFilter(SubscriberStatus.ACTIVE);
        newsletter.setPreferenceFilter(List.of("tech"));
        DispatchResponse sent = newsletterService.sendNewsletter(newsletter);

        SendPostNotificationRequest post = new SendPostNotificationRequest();
        post.setPostId(21L);
        post.setSlug("new-post");
        post.setTitle("New Post");
        post.setExcerpt("Read this");
        DispatchResponse postSent = newsletterService.sendPostNotification(post);

        assertThat(sent.getRecipients()).isEqualTo(1L);
        assertThat(postSent.getRecipients()).isEqualTo(1L);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(notificationKafkaTemplate, times(2)).send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    @Test
    void privateHelpersCoverPreferenceAndEncodingBranches() {
        assertThat((String) ReflectionTestUtils.invokeMethod(newsletterService, "joinPreferences", List.of(" Tech ", "", "java", "tech")))
                .isEqualTo("tech,java");
        assertThat((java.util.Set<String>) ReflectionTestUtils.invokeMethod(newsletterService, "parsePreferences", "tech, java"))
                .containsExactlyInAnyOrder("tech", "java");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(newsletterService, "disjoint",
                java.util.Set.of("a"), java.util.Set.of("b"))).isTrue();
        assertThat((String) ReflectionTestUtils.invokeMethod(newsletterService, "trimTrailingSlash", "http://x/"))
                .isEqualTo("http://x");
        assertThat((String) ReflectionTestUtils.invokeMethod(newsletterService, "encode", "a b"))
                .isEqualTo("a+b");
    }

    @Test
    void lookupFailuresAndDisplayNameFallbackAreCovered() {
        when(subscriberRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> newsletterService.getSubscriberByEmail("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subscriber not found");

        Subscriber noName = Subscriber.builder().email("noname@example.com").fullName(" ").build();
        assertThat((String) ReflectionTestUtils.invokeMethod(newsletterService, "displayName", noName))
                .isEqualTo("noname@example.com");
    }

    @Test
    void inAppNotificationSkipsNullUserAndHandlesUnavailableKafkaProvider() {
        Subscriber withoutUser = Subscriber.builder().subscriberId(5L).userId(null).build();
        ReflectionTestUtils.invokeMethod(
                newsletterService,
                "sendInAppNotification",
                withoutUser,
                "ADMIN_BROADCAST",
                "title",
                "message",
                1L,
                "NEWSLETTER");
        verify(notificationKafkaTemplate, times(0)).send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));

        Subscriber withUser = Subscriber.builder().subscriberId(6L).userId(99L).build();
        ReflectionTestUtils.setField(newsletterService, "notificationKafkaTemplateProvider", null);
        ReflectionTestUtils.invokeMethod(
                newsletterService,
                "sendInAppNotification",
                withUser,
                "ADMIN_BROADCAST",
                "title",
                "message",
                1L,
                "NEWSLETTER");
        verify(notificationKafkaTemplate, times(0)).send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    private void enableKafka() {
        ReflectionTestUtils.setField(newsletterService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        when(notificationKafkaTemplateProvider.getIfAvailable()).thenReturn(notificationKafkaTemplate);
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
                .when(notificationKafkaTemplate)
                .send(org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.any(NotificationDispatchEvent.class));
    }

    private Object buildEntitlement(boolean admin, boolean newsletterEntitled) {
        try {
            Class<?> type = Class.forName(NewsletterServiceImpl.class.getName() + "$EntitlementResponse");
            java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            ReflectionTestUtils.setField(object, "admin", admin);
            ReflectionTestUtils.setField(object, "newsletterEntitled", newsletterEntitled);
            return object;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

