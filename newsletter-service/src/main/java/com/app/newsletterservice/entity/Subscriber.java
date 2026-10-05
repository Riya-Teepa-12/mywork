package com.app.newsletterservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscribers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriberId;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    private Long userId;

    @Column(length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriberStatus status;

    private LocalDateTime subscribedAt;

    private LocalDateTime unsubscribedAt;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    private LocalDateTime tokenExpiresAt;

    @Column(length = 1000)
    private String preferences;

    @PrePersist
    public void prePersist() {
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SubscriberStatus.PENDING;
        }
    }
}
