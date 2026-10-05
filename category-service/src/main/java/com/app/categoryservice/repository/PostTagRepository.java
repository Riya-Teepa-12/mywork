package com.app.categoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.categoryservice.entity.PostTag;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    List<PostTag> findByPostId(Long postId);
    Optional<PostTag> findByPostIdAndTagId(Long postId, Long tagId);
    void deleteByPostIdAndTagId(Long postId, Long tagId);
}
