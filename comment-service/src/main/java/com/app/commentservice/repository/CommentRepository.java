package com.app.commentservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.commentservice.entity.Comment;
import com.app.commentservice.entity.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
    List<Comment> findByAuthorId(Long authorId);
    Optional<Comment> findByCommentId(Long commentId);
    List<Comment> findByParentCommentId(Long parentCommentId);
    @Query("select c from Comment c where c.postId = :postId and c.parentCommentId is null order by c.createdAt asc")
    List<Comment> findTopLevelByPostId(@Param("postId") Long postId);
    long countByPostId(Long postId);
    List<Comment> findByStatus(CommentStatus status);
    void deleteByPostId(Long postId);
    void deleteByCommentId(Long commentId);
    void deleteByCommentIdIn(Collection<Long> commentIds);
    long countByPostIdAndStatusNot(Long postId, CommentStatus status);
    long countByStatusNot(CommentStatus status);

    @Modifying
    @Query("update Comment c set c.likesCount = c.likesCount + 1 where c.commentId = :commentId")
    int incrementLikes(@Param("commentId") Long commentId);

    @Modifying
    @Query("update Comment c set c.likesCount = case when c.likesCount > 0 then c.likesCount - 1 else 0 end where c.commentId = :commentId")
    int decrementLikes(@Param("commentId") Long commentId);
}
