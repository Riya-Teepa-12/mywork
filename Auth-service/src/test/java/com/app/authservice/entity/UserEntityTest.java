package com.app.authservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UserEntityTest {

    @Test
    void onCreateAppliesCreatedAt() {
        User user = User.builder().build();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
    }

    @Test
    void subscriptionAndOrderEntitiesApplyTimestampDefaults() {
        UserSubscription subscription = UserSubscription.builder().build();
        subscription.prePersist();
        assertNotNull(subscription.getCreatedAt());
        assertNotNull(subscription.getUpdatedAt());

        PaymentOrder order = PaymentOrder.builder().build();
        order.prePersist();
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());

        EmailOtp otp = EmailOtp.builder().build();
        otp.prePersist();
        assertEquals(false, otp.getConsumed());
        assertEquals(0, otp.getAttemptCount());
        assertNotNull(otp.getCreatedAt());

        AuditLog auditLog = AuditLog.builder().build();
        auditLog.prePersist();
        assertNotNull(auditLog.getCreatedAt());
    }
}

