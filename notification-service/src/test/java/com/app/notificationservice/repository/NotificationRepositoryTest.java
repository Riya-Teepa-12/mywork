package com.app.notificationservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.notificationservice.entity.AppUser;
import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.entity.NotificationType;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notificationdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void repositorySupportsCommonQueries() {
        AppUser user = new AppUser();
        user.setUserId(10L);
        user.setEmail("reader@example.com");
        user.setFullName("Reader");
        user.setActive(true);
        appUserRepository.saveAndFlush(user);

        Notification notification = notificationRepository.saveAndFlush(Notification.builder()
                .recipientId(10L)
                .actorId(11L)
                .type(NotificationType.NEW_POST)
                .title("Title")
                .message("Message")
                .relatedId(1L)
                .relatedType("POST")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        assertThat(notificationRepository.findByRecipientId(10L)).contains(notification);
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L)).contains(notification);
        assertThat(notificationRepository.findByRecipientIdAndIsRead(10L, false)).contains(notification);
        assertThat(notificationRepository.countByRecipientIdAndIsRead(10L, false)).isEqualTo(1L);
        assertThat(notificationRepository.findByType(NotificationType.NEW_POST)).contains(notification);
        assertThat(notificationRepository.findByRelatedId(1L)).contains(notification);
        assertThat(notificationRepository.findAllByOrderByCreatedAtDesc()).contains(notification);
        assertThat(appUserRepository.findByUserId(10L)).contains(user);
    }
}

