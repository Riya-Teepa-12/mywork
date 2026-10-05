package com.app.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.DispatchResponse;
import com.app.notificationservice.dto.NotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.entity.AppUser;
import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.entity.NotificationType;
import com.app.notificationservice.repository.AppUserRepository;
import com.app.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "mailFrom", "notify@inkwell.com");
        ReflectionTestUtils.setField(notificationService, "unreadCachePrefix", "notification:unread");
        ReflectionTestUtils.setField(notificationService, "unreadCacheTtlSeconds", 120L);
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    void sendMarkReadAndUnreadCountWork() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipientId(10L)
                .actorId(11L)
                .type(NotificationType.NEW_POST)
                .title("Title")
                .message("Message")
                .relatedId(1L)
                .relatedType("POST")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationRepository.findById(1L)).thenReturn(java.util.Optional.of(notification));
        when(notificationRepository.findByRecipientIdAndIsRead(10L, false)).thenReturn(List.of(notification));
        when(notificationRepository.countByRecipientIdAndIsRead(10L, false)).thenReturn(1L);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(notification));
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notification));
        when(notificationRepository.findByRecipientIdAndIsRead(10L, true)).thenReturn(List.of(notification));

        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(10L);
        request.setActorId(11L);
        request.setType(NotificationType.NEW_POST);
        request.setTitle("Title");
        request.setMessage("Message");
        request.setRelatedId(1L);
        request.setRelatedType("POST");

        NotificationResponse response = notificationService.send(request);
        assertThat(response.getNotificationId()).isEqualTo(1L);
        assertThat(notificationService.markAsRead(1L).getRead()).isTrue();
        assertThat(notificationService.markAllRead(10L).getCount()).isEqualTo(1L);
        assertThat(notificationService.deleteRead(10L).getCount()).isEqualTo(1L);
        assertThat(notificationService.getUnreadCount(10L)).isEqualTo(1L);
        assertThat(notificationService.getByRecipient(10L)).hasSize(1);
        assertThat(notificationService.getAll()).hasSize(1);
    }

    @Test
    void sendBulkAndEmailUseRepositoryAndMailSender() {
        Notification notification = Notification.builder()
                .notificationId(2L)
                .recipientId(20L)
                .actorId(11L)
                .type(NotificationType.NEW_COMMENT)
                .title("Comment")
                .message("New comment")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.saveAll(any())).thenReturn(List.of(notification));
        AppUser recipient = new AppUser();
        recipient.setUserId(20L);
        recipient.setEmail("user@example.com");
        recipient.setFullName("User");
        recipient.setActive(true);
        when(appUserRepository.findByUserId(20L)).thenReturn(java.util.Optional.of(recipient));

        BulkNotificationRequest bulk = new BulkNotificationRequest();
        bulk.setRecipientIds(List.of(20L));
        bulk.setActorId(11L);
        bulk.setType(NotificationType.NEW_COMMENT);
        bulk.setTitle("Comment");
        bulk.setMessage("New comment");

        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(20L);
        request.setActorId(11L);
        request.setType(NotificationType.NEW_COMMENT);
        request.setTitle("Comment");
        request.setMessage("New comment");
        request.setRelatedId(5L);
        request.setRelatedType("COMMENT");

        DispatchResponse bulkResponse = notificationService.sendBulk(bulk);
        DispatchResponse emailResponse = notificationService.sendEmail(request);

        assertThat(bulkResponse.getCount()).isEqualTo(1L);
        assertThat(emailResponse.getCount()).isEqualTo(1L);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void getUnreadCountUsesCacheWhenAvailable() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notification:unread:55")).thenReturn("4");

        long count = notificationService.getUnreadCount(55L);

        assertThat(count).isEqualTo(4L);
    }

    @Test
    void getUnreadCountWritesToCacheWhenRepositoryUsed() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notification:unread:88")).thenReturn(null);
        when(notificationRepository.countByRecipientIdAndIsRead(88L, false)).thenReturn(3L);

        long count = notificationService.getUnreadCount(88L);

        assertThat(count).isEqualTo(3L);
        verify(valueOperations).set(any(), any(), any());
    }

    @Test
    void sendEmailValidatesRecipientState() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(20L);
        request.setType(NotificationType.NEW_COMMENT);
        request.setTitle("Comment");
        request.setMessage("New comment");

        when(appUserRepository.findByUserId(20L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> notificationService.sendEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipient user not found");

        AppUser inactive = new AppUser();
        inactive.setUserId(20L);
        inactive.setEmail("inactive@example.com");
        inactive.setActive(false);
        when(appUserRepository.findByUserId(20L)).thenReturn(java.util.Optional.of(inactive));
        assertThatThrownBy(() -> notificationService.sendEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");

        AppUser missingEmail = new AppUser();
        missingEmail.setUserId(20L);
        missingEmail.setActive(true);
        missingEmail.setEmail(" ");
        when(appUserRepository.findByUserId(20L)).thenReturn(java.util.Optional.of(missingEmail));
        assertThatThrownBy(() -> notificationService.sendEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not set");
    }

    @Test
    void deleteNotificationUsesRepositoryDelete() {
        Notification notification = Notification.builder()
                .notificationId(100L)
                .recipientId(33L)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findById(100L)).thenReturn(java.util.Optional.of(notification));

        DispatchResponse response = notificationService.deleteNotification(100L);

        assertThat(response.getCount()).isEqualTo(1L);
        verify(notificationRepository).deleteByNotificationId(100L);
    }
}

