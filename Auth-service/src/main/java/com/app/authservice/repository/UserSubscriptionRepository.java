package com.app.authservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;
import com.app.authservice.entity.UserSubscription;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select s from UserSubscription s
            where s.userId = :userId
              and s.planType = :planType
              and s.status = :status
              and (s.endsAt is null or s.endsAt > :now)
            order by s.endsAt desc, s.subscriptionId desc
            """)
    List<UserSubscription> findActiveByUserIdAndPlanType(
            @Param("userId") Long userId,
            @Param("planType") SubscriptionPlanType planType,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);

    Optional<UserSubscription> findTopByUserIdAndPlanTypeOrderBySubscriptionIdDesc(Long userId, SubscriptionPlanType planType);
}

