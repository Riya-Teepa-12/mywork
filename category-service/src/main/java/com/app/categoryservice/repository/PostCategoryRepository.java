package com.app.categoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.categoryservice.entity.PostCategory;

public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    List<PostCategory> findByPostId(Long postId);
    Optional<PostCategory> findByPostIdAndCategoryId(Long postId, Long categoryId);
    void deleteByPostIdAndCategoryId(Long postId, Long categoryId);
}
