package com.app.categoryservice.service;

import java.util.List;

import com.app.categoryservice.dto.CategoryRequest;
import com.app.categoryservice.dto.CategoryResponse;
import com.app.categoryservice.dto.PostTaxonomyRequest;
import com.app.categoryservice.dto.TagRequest;
import com.app.categoryservice.dto.TagResponse;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getBySlug(String slug);
    List<CategoryResponse> getAllCategories();
    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);
    TagResponse createTag(TagRequest request);
    TagResponse updateTag(Long tagId, TagRequest request);
    TagResponse getTagBySlug(String slug);
    List<TagResponse> getAllTags();
    void deleteTag(Long tagId);
    void addTagToPost(PostTaxonomyRequest request);
    void removeTagFromPost(PostTaxonomyRequest request);
    List<TagResponse> getTagsByPost(Long postId);
    List<TagResponse> getTrendingTags();
    void addCategoryToPost(PostTaxonomyRequest request);
    void removeCategoryFromPost(PostTaxonomyRequest request);
    List<CategoryResponse> getCategoriesByPost(Long postId);
}
