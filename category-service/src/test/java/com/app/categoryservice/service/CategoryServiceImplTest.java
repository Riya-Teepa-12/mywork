package com.app.categoryservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostTagRepository postTagRepository;

    @Mock
    private PostCategoryRepository postCategoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createCategoryAndTagPersistExpectedSlugs() {
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Technology");
        categoryRequest.setDescription("Tech topics");
        when(categoryRepository.existsBySlug("technology")).thenReturn(false);
        when(tagRepository.existsBySlug("technology")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setCategoryId(1L);
            category.setCreatedAt(LocalDateTime.now());
            return category;
        });

        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);

        assertThat(categoryResponse.getSlug()).isEqualTo("technology");

        TagRequest tagRequest = new TagRequest();
        tagRequest.setName("Java");
        when(tagRepository.existsBySlug("java")).thenReturn(false);
        when(categoryRepository.existsBySlug("java")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setTagId(2L);
            tag.setCreatedAt(LocalDateTime.now());
            return tag;
        });

        TagResponse tagResponse = categoryService.createTag(tagRequest);
        assertThat(tagResponse.getSlug()).isEqualTo("java");
    }

    @Test
    void addAndRemoveTaxonomyLinksAdjustCounts() {
        Category category = Category.builder().categoryId(3L).name("News").slug("news").postCount(0L).createdAt(LocalDateTime.now()).build();
        Tag tag = Tag.builder().tagId(4L).name("Spring").slug("spring").postCount(0L).createdAt(LocalDateTime.now()).build();
        when(categoryRepository.findByCategoryId(3L)).thenReturn(java.util.Optional.of(category));
        when(tagRepository.findByTagId(4L)).thenReturn(java.util.Optional.of(tag));
        when(postCategoryRepository.findByPostIdAndCategoryId(5L, 3L)).thenReturn(java.util.Optional.empty());
        when(postTagRepository.findByPostIdAndTagId(5L, 4L)).thenReturn(java.util.Optional.empty());
        when(postCategoryRepository.save(any(PostCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postTagRepository.save(any(PostTag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostTaxonomyRequest request = new PostTaxonomyRequest();
        request.setPostId(5L);
        request.setTaxonomyId(3L);
        categoryService.addCategoryToPost(request);
        categoryService.removeCategoryFromPost(request);

        request.setTaxonomyId(4L);
        categoryService.addTagToPost(request);
        categoryService.removeTagFromPost(request);

        verify(categoryRepository).save(category);
        verify(tagRepository).save(tag);
    }

    @Test
    void getListsUseRepositoryMappings() {
        Category category = Category.builder().categoryId(3L).name("News").slug("news").postCount(0L).createdAt(LocalDateTime.now()).build();
        Tag tag = Tag.builder().tagId(4L).name("Spring").slug("spring").postCount(0L).createdAt(LocalDateTime.now()).build();
        when(postCategoryRepository.findByPostId(5L)).thenReturn(List.of(PostCategory.builder().postId(5L).categoryId(3L).build()));
        when(postTagRepository.findByPostId(5L)).thenReturn(List.of(PostTag.builder().postId(5L).tagId(4L).build()));
        when(categoryRepository.findByCategoryId(3L)).thenReturn(java.util.Optional.of(category));
        when(tagRepository.findByTagId(4L)).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.findTop10ByOrderByPostCountDescCreatedAtDesc()).thenReturn(List.of(tag));

        assertThat(categoryService.getCategoriesByPost(5L)).hasSize(1);
        assertThat(categoryService.getTagsByPost(5L)).hasSize(1);
        assertThat(categoryService.getTrendingTags()).hasSize(1);
    }

    @Test
    void updateDeleteAndFetchOperationsCoverAdditionalBranches() {
        Category existing = Category.builder()
                .categoryId(9L)
                .name("Old Name")
                .slug("old-name")
                .description("old")
                .parentCategoryId(null)
                .postCount(1L)
                .createdAt(LocalDateTime.now())
                .build();
        Category parent = Category.builder()
                .categoryId(1L)
                .name("Parent")
                .slug("parent")
                .postCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
        when(categoryRepository.findByCategoryId(9L)).thenReturn(java.util.Optional.of(existing));
        when(categoryRepository.findByCategoryId(1L)).thenReturn(java.util.Optional.of(parent));
        when(categoryRepository.existsBySlug("new-name")).thenReturn(true);
        when(categoryRepository.existsBySlug("new-name-2")).thenReturn(false);
        when(tagRepository.existsBySlug("new-name-2")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRequest update = new CategoryRequest();
        update.setName("New Name");
        update.setDescription("desc");
        update.setParentCategoryId(1L);
        assertThat(categoryService.updateCategory(9L, update).getSlug()).isEqualTo("new-name-2");

        when(postCategoryRepository.findAll()).thenReturn(List.of(
                PostCategory.builder().postId(1L).categoryId(9L).build(),
                PostCategory.builder().postId(2L).categoryId(88L).build()));
        categoryService.deleteCategory(9L);
        verify(postCategoryRepository).deleteAll(any(List.class));
        verify(categoryRepository).delete(existing);

        when(categoryRepository.findBySlug("parent")).thenReturn(java.util.Optional.of(parent));
        when(categoryRepository.findAll()).thenReturn(List.of(parent));
        assertThat(categoryService.getBySlug("parent").getCategoryId()).isEqualTo(1L);
        assertThat(categoryService.getAllCategories()).hasSize(1);
    }

    @Test
    void validationAndDuplicateLinkBranchesAreHandled() {
        Category self = Category.builder()
                .categoryId(20L)
                .name("Self")
                .slug("self")
                .createdAt(LocalDateTime.now())
                .build();
        when(categoryRepository.findByCategoryId(20L)).thenReturn(java.util.Optional.of(self));

        CategoryRequest invalid = new CategoryRequest();
        invalid.setName("Self");
        invalid.setParentCategoryId(20L);
        assertThatThrownBy(() -> categoryService.updateCategory(20L, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be parent");

        Tag tag = Tag.builder().tagId(7L).name("Java").slug("java").postCount(3L).createdAt(LocalDateTime.now()).build();
        when(tagRepository.findByTagId(7L)).thenReturn(java.util.Optional.of(tag));
        PostTaxonomyRequest request = new PostTaxonomyRequest();
        request.setPostId(5L);
        request.setTaxonomyId(7L);
        when(postTagRepository.findByPostIdAndTagId(5L, 7L))
                .thenReturn(java.util.Optional.of(PostTag.builder().postId(5L).tagId(7L).build()))
                .thenReturn(java.util.Optional.of(PostTag.builder().postId(5L).tagId(7L).build()));

        categoryService.addTagToPost(request);
        categoryService.removeTagFromPost(request);
        verify(tagRepository).save(tag);

        when(tagRepository.findBySlug("java")).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        assertThat(categoryService.getTagBySlug("java").getTagId()).isEqualTo(7L);
        assertThat(categoryService.getAllTags()).hasSize(1);
    }
}

