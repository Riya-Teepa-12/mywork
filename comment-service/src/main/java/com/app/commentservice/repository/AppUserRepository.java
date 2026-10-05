package com.app.commentservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.commentservice.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUserId(Long userId);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
