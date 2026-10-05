package com.app.categoryservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.categoryservice.dto.CategoryRequest;
import com.app.categoryservice.dto.CategoryResponse;
import com.app.categoryservice.dto.PostTaxonomyRequest;
import com.app.categoryservice.dto.TagRequest;
import com.app.categoryservice.dto.TagResponse;
import com.app.categoryservice.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CategoryResource {

    private final CategoryService categoryService;

    @PostMapping("/categories")
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        return categoryService.createCategory(request);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/categories/{slug}")
    public CategoryResponse getCategoryBySlug(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    public Map<String, String> deleteCategory(
            @PathVariable Long categoryId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        categoryService.deleteCategory(categoryId);
        return Map.of("message", "Category deleted");
    }

    @PostMapping("/tags")
    public TagResponse createTag(
            @Valid @RequestBody TagRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        return categoryService.createTag(request);
    }

    @GetMapping("/tags")
    public List<TagResponse> getAllTags() {
        return categoryService.getAllTags();
    }

    @GetMapping("/tags/{slug}")
    public TagResponse getTagBySlug(@PathVariable String slug) {
        return categoryService.getTagBySlug(slug);
    }

    @PutMapping("/tags/{tagId}")
    public TagResponse updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return categoryService.updateTag(tagId, request);
    }

    @DeleteMapping("/tags/{tagId}")
    public Map<String, String> deleteTag(
            @PathVariable Long tagId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        categoryService.deleteTag(tagId);
        return Map.of("message", "Tag deleted");
    }

    @PostMapping("/tags/post")
    public Map<String, String> addTagToPost(
            @Valid @RequestBody PostTaxonomyRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        categoryService.addTagToPost(request);
        return Map.of("message", "Tag added to post");
    }

    @DeleteMapping("/tags/post")
    public Map<String, String> removeTagFromPost(
            @Valid @RequestBody PostTaxonomyRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        categoryService.removeTagFromPost(request);
        return Map.of("message", "Tag removed from post");
    }

    @GetMapping("/tags/post/{postId}")
    public List<TagResponse> getTagsByPost(@PathVariable Long postId) {
        return categoryService.getTagsByPost(postId);
    }

    @GetMapping("/tags/trending")
    public List<TagResponse> getTrendingTags() {
        return categoryService.getTrendingTags();
    }

    @PostMapping("/categories/post")
    public Map<String, String> addCategoryToPost(
            @Valid @RequestBody PostTaxonomyRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        categoryService.addCategoryToPost(request);
        return Map.of("message", "Category added to post");
    }

    @DeleteMapping("/categories/post")
    public Map<String, String> removeCategoryFromPost(
            @Valid @RequestBody PostTaxonomyRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorRole);
        categoryService.removeCategoryFromPost(request);
        return Map.of("message", "Category removed from post");
    }

    @GetMapping("/categories/post/{postId}")
    public List<CategoryResponse> getCategoriesByPost(@PathVariable Long postId) {
        return categoryService.getCategoriesByPost(postId);
    }

    private void requireAdmin(String actorRole) {
        if (!"ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
    }

    private void requireAuthorOrAdmin(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase();
        if (!"ADMIN".equals(normalized) && !"AUTHOR".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Author or admin role is required");
        }
    }
}
