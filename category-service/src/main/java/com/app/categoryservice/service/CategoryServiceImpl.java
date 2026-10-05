package com.app.categoryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.categoryservice.dto.CategoryRequest;
import com.app.categoryservice.dto.CategoryResponse;
import com.app.categoryservice.dto.PostTaxonomyRequest;
import com.app.categoryservice.dto.TagRequest;
import com.app.categoryservice.dto.TagResponse;
import com.app.categoryservice.entity.Category;
import com.app.categoryservice.entity.PostCategory;
import com.app.categoryservice.entity.PostTag;
import com.app.categoryservice.entity.Tag;
import com.app.categoryservice.repository.CategoryRepository;
import com.app.categoryservice.repository.PostCategoryRepository;
import com.app.categoryservice.repository.PostTagRepository;
import com.app.categoryservice.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final PostCategoryRepository postCategoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        validateParent(request.getParentCategoryId(), null);
        String slug = generateUniqueSlug(request.getName());
        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .parentCategoryId(request.getParentCategoryId())
                .build();
        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        return toCategoryResponse(findCategoryBySlug(slug));
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toCategoryResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findCategory(categoryId);
        validateParent(request.getParentCategoryId(), categoryId);
        if (!category.getName().equalsIgnoreCase(request.getName().trim())) {
            category.setName(request.getName().trim());
            category.setSlug(generateUniqueSlug(request.getName()));
        }
        category.setDescription(request.getDescription());
        category.setParentCategoryId(request.getParentCategoryId());
        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategory(categoryId);
        List<PostCategory> links = postCategoryRepository.findAll().stream()
                .filter(link -> link.getCategoryId().equals(categoryId))
                .toList();
        postCategoryRepository.deleteAll(links);
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        String slug = generateUniqueSlug(request.getName());
        Tag tag = Tag.builder()
                .name(request.getName().trim())
                .slug(slug)
                .build();
        tag = tagRepository.save(tag);
        return toTagResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long tagId, TagRequest request) {
        Tag tag = findTag(tagId);
        String nextName = request.getName().trim();
        if (!tag.getName().equalsIgnoreCase(nextName)) {
            tag.setName(nextName);
            tag.setSlug(generateUniqueSlug(nextName));
        }
        tag = tagRepository.save(tag);
        return toTagResponse(tag);
    }

    @Override
    public TagResponse getTagBySlug(String slug) {
        return toTagResponse(findTagBySlug(slug));
    }

    @Override
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(this::toTagResponse).toList();
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = findTag(tagId);
        List<PostTag> links = postTagRepository.findAll().stream()
                .filter(link -> link.getTagId().equals(tagId))
                .toList();
        postTagRepository.deleteAll(links);
        tagRepository.delete(tag);
    }

    @Override
    @Transactional
    public void addTagToPost(PostTaxonomyRequest request) {
        Tag tag = findTag(request.getTaxonomyId());
        if (postTagRepository.findByPostIdAndTagId(request.getPostId(), tag.getTagId()).isPresent()) {
            return;
        }
        postTagRepository.save(PostTag.builder().postId(request.getPostId()).tagId(tag.getTagId()).build());
        tag.setPostCount(tag.getPostCount() + 1);
        tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void removeTagFromPost(PostTaxonomyRequest request) {
        Tag tag = findTag(request.getTaxonomyId());
        PostTag link = postTagRepository.findByPostIdAndTagId(request.getPostId(), tag.getTagId()).orElse(null);
        if (link == null) {
            return;
        }
        postTagRepository.delete(link);
        tag.setPostCount(tag.getPostCount() > 0 ? tag.getPostCount() - 1 : 0);
        tagRepository.save(tag);
    }

    @Override
    public List<TagResponse> getTagsByPost(Long postId) {
        return postTagRepository.findByPostId(postId).stream()
                .map(link -> findTag(link.getTagId()))
                .map(this::toTagResponse)
                .toList();
    }

    @Override
    public List<TagResponse> getTrendingTags() {
        return tagRepository.findTop10ByOrderByPostCountDescCreatedAtDesc().stream()
                .map(this::toTagResponse)
                .toList();
    }

    @Override
    @Transactional
    public void addCategoryToPost(PostTaxonomyRequest request) {
        Category category = findCategory(request.getTaxonomyId());
        if (postCategoryRepository.findByPostIdAndCategoryId(request.getPostId(), category.getCategoryId()).isPresent()) {
            return;
        }
        postCategoryRepository.save(PostCategory.builder()
                .postId(request.getPostId())
                .categoryId(category.getCategoryId())
                .build());
        category.setPostCount(category.getPostCount() + 1);
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void removeCategoryFromPost(PostTaxonomyRequest request) {
        Category category = findCategory(request.getTaxonomyId());
        PostCategory link = postCategoryRepository.findByPostIdAndCategoryId(request.getPostId(), category.getCategoryId())
                .orElse(null);
        if (link == null) {
            return;
        }
        postCategoryRepository.delete(link);
        category.setPostCount(category.getPostCount() > 0 ? category.getPostCount() - 1 : 0);
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> getCategoriesByPost(Long postId) {
        return postCategoryRepository.findByPostId(postId).stream()
                .map(link -> findCategory(link.getCategoryId()))
                .map(this::toCategoryResponse)
                .toList();
    }

    private Category findCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private Tag findTagBySlug(String slug) {
        return tagRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private Tag findTag(Long tagId) {
        return tagRepository.findByTagId(tagId).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private void validateParent(Long parentCategoryId, Long selfCategoryId) {
        if (parentCategoryId == null) {
            return;
        }
        if (selfCategoryId != null && selfCategoryId.equals(parentCategoryId)) {
            throw new IllegalArgumentException("Category cannot be parent of itself");
        }
        findCategory(parentCategoryId);
    }

    private String generateUniqueSlug(String value) {
        String base = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) {
            base = "taxonomy";
        }
        String slug = base;
        int suffix = 2;
        while (!slugAvailable(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private boolean slugAvailable(String slug) {
        return !categoryRepository.existsBySlug(slug) && !tagRepository.existsBySlug(slug);
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategoryId())
                .postCount(category.getPostCount())
                .createdAt(category.getCreatedAt())
                .build();
    }

    private TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount(tag.getPostCount())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
