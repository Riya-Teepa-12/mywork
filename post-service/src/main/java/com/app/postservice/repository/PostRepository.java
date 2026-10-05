package com.app.postservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.postservice.entity.Post;
import com.app.postservice.entity.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    List<Post> findByAuthorId(Long authorId);

    List<Post> findByAuthorIdAndStatusOrderByPublishedAtDesc(Long authorId, PostStatus status);

    List<Post> findByStatus(PostStatus status);

    Optional<Post> findByPostId(Long postId);

    @Query("select p from Post p where lower(p.title) like lower(concat('%', :keyword, '%')) or lower(p.content) like lower(concat('%', :keyword, '%'))")
    List<Post> searchByTitle(@Param("keyword") String keyword);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Query("select p from Post p where p.status='PUBLISHED' order by p.featured desc, p.publishedAt desc")
    List<Post> findPublishedOrderByPublishedAtDesc();

    List<Post> findAllByOrderByUpdatedAtDesc();

    List<Post> findAllByOrderByViewCountDescPublishedAtDesc();

    long countByAuthorId(Long authorId);

    boolean existsBySlug(String slug);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.postId = :postId")
    int incrementViews(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p set p.likesCount = p.likesCount + 1 where p.postId = :postId")
    int incrementLikes(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p set p.likesCount = case when p.likesCount > 0 then p.likesCount - 1 else 0 end where p.postId = :postId")
    int decrementLikes(@Param("postId") Long postId);
}
