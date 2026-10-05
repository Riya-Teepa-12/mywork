package com.app.commentservice.repository;

import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.commentservice.entity.CommentLike;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdIn(Collection<Long> commentIds);
}
