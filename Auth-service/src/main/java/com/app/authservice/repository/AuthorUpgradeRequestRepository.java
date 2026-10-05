package com.app.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.authservice.entity.AuthorUpgradeRequest;
import com.app.authservice.entity.AuthorUpgradeStatus;

public interface AuthorUpgradeRequestRepository extends JpaRepository<AuthorUpgradeRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, AuthorUpgradeStatus status);

    Optional<AuthorUpgradeRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuthorUpgradeRequest> findByStatusOrderByCreatedAtDesc(AuthorUpgradeStatus status);

    List<AuthorUpgradeRequest> findAllByOrderByCreatedAtDesc();
}
