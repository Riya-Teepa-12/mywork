package com.app.authservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "author_upgrade_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(length = 120)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 2000)
    private String bio;

    @Column(nullable = false, length = 2000)
    private String motivation;

    @Column(nullable = false, length = 2000)
    private String expertiseCategories;

    @Column(nullable = false, length = 2000)
    private String writingSampleUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthorUpgradeStatus status;

    @Column(length = 1000)
    private String decisionReason;

    private Long reviewedByUserId;

    @Column(length = 120)
    private String reviewedByName;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AuthorUpgradeStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
