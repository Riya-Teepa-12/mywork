package com.app.categoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.categoryservice.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByCategoryId(Long categoryId);
    List<Category> findByParentCategoryId(Long parentCategoryId);
    boolean existsBySlug(String slug);
}
