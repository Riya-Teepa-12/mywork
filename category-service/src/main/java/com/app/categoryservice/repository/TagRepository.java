package com.app.categoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.categoryservice.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
    Optional<Tag> findByTagId(Long tagId);
    boolean existsBySlug(String slug);
    List<Tag> findTop10ByOrderByPostCountDescCreatedAtDesc();
}
