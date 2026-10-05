package com.app.postservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.postservice.entity.AuthorFollow;

public interface AuthorFollowRepository extends JpaRepository<AuthorFollow, Long> {
    Optional<AuthorFollow> findByAuthorIdAndFollowerId(Long authorId, Long followerId);
    List<AuthorFollow> findByAuthorId(Long authorId);
    List<AuthorFollow> findByFollowerId(Long followerId);
    long countByAuthorId(Long authorId);
}
