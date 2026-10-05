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
@Table(name = "payment_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentOrderId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionPlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOrderStatus status;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 120, unique = true)
    private String providerOrderId;

    @Column(length = 120)
    private String providerPaymentId;

    @Column(length = 255)
    private String providerSignature;

    @Column(length = 120, unique = true)
    private String receipt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

