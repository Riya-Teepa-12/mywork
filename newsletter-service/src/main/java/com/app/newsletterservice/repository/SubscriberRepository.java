package com.app.newsletterservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.newsletterservice.entity.Subscriber;
import com.app.newsletterservice.entity.SubscriberStatus;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmail(String email);
    Optional<Subscriber> findByUserId(Long userId);
    List<Subscriber> findByStatus(SubscriberStatus status);
    Optional<Subscriber> findByToken(String token);
    boolean existsByEmail(String email);
    long countByStatus(SubscriberStatus status);
    void deleteBySubscriberId(Long subscriberId);
}
