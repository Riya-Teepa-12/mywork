package com.app.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.authservice.entity.PaymentOrder;
import com.app.authservice.entity.PaymentOrderStatus;
import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;
import com.app.authservice.entity.UserSubscription;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan(basePackageClasses = UserSubscription.class)
class SubscriptionRepositoryTest {

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Test
    void repositoriesSupportSubscriptionLookups() {
        UserSubscription subscription = userSubscriptionRepository.saveAndFlush(UserSubscription.builder()
                .userId(1L)
                .planType(SubscriptionPlanType.AUTHOR_POSTS)
                .status(SubscriptionStatus.ACTIVE)
                .amountPaise(100L)
                .currency("INR")
                .providerOrderId("order-1")
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build());

        PaymentOrder order = paymentOrderRepository.saveAndFlush(PaymentOrder.builder()
                .userId(1L)
                .planType(SubscriptionPlanType.AUTHOR_POSTS)
                .status(PaymentOrderStatus.CREATED)
                .amountPaise(100L)
                .currency("INR")
                .providerOrderId("provider-1")
                .receipt("receipt-1")
                .build());

        assertThat(userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(1L)).contains(subscription);
        assertThat(userSubscriptionRepository.findActiveByUserIdAndPlanType(1L, SubscriptionPlanType.AUTHOR_POSTS, SubscriptionStatus.ACTIVE, LocalDateTime.now())).contains(subscription);
        assertThat(userSubscriptionRepository.findTopByUserIdAndPlanTypeOrderBySubscriptionIdDesc(1L, SubscriptionPlanType.AUTHOR_POSTS)).contains(subscription);
        assertThat(paymentOrderRepository.findByProviderOrderId("provider-1")).contains(order);
    }
}

