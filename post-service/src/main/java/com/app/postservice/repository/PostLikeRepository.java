package com.app.postservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.postservice.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
}
