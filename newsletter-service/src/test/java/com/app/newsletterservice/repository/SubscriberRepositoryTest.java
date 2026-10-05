package com.app.newsletterservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.newsletterservice.entity.Subscriber;
import com.app.newsletterservice.entity.SubscriberStatus;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:newsletterdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SubscriberRepositoryTest {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Test
    void repositorySupportsCommonQueries() {
        Subscriber subscriber = subscriberRepository.saveAndFlush(Subscriber.builder()
                .email("reader@example.com")
                .userId(10L)
                .fullName("Reader")
                .status(SubscriberStatus.ACTIVE)
                .subscribedAt(LocalDateTime.now())
                .token("token-1")
                .tokenExpiresAt(LocalDateTime.now().plusDays(1))
                .preferences("tech,java")
                .build());

        assertThat(subscriberRepository.findByEmail("reader@example.com")).contains(subscriber);
        assertThat(subscriberRepository.findByUserId(10L)).contains(subscriber);
        assertThat(subscriberRepository.findByStatus(SubscriberStatus.ACTIVE)).contains(subscriber);
        assertThat(subscriberRepository.findByToken("token-1")).contains(subscriber);
        assertThat(subscriberRepository.existsByEmail("reader@example.com")).isTrue();
        assertThat(subscriberRepository.countByStatus(SubscriberStatus.ACTIVE)).isEqualTo(1L);
    }
}

