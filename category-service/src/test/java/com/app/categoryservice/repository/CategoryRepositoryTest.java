package com.app.categoryservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.categoryservice.entity.Category;
import com.app.categoryservice.entity.PostCategory;
import com.app.categoryservice.entity.PostTag;
import com.app.categoryservice.entity.Tag;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:categorydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Test
    void repositoriesSupportLookupAndDeleteQueries() {
        Category category = categoryRepository.saveAndFlush(category("Technology", "technology"));
        Tag tag = tagRepository.saveAndFlush(tag("Java", "java"));
        PostCategory postCategory = postCategoryRepository.saveAndFlush(PostCategory.builder().postId(1L).categoryId(category.getCategoryId()).build());
        PostTag postTag = postTagRepository.saveAndFlush(PostTag.builder().postId(1L).tagId(tag.getTagId()).build());

        assertThat(categoryRepository.findBySlug("technology")).contains(category);
        assertThat(categoryRepository.findByCategoryId(category.getCategoryId())).contains(category);
        assertThat(categoryRepository.findByParentCategoryId(null)).isNotNull();
        assertThat(categoryRepository.existsBySlug("technology")).isTrue();

        assertThat(tagRepository.findBySlug("java")).contains(tag);
        assertThat(tagRepository.findByTagId(tag.getTagId())).contains(tag);
        assertThat(tagRepository.existsBySlug("java")).isTrue();
        assertThat(tagRepository.findTop10ByOrderByPostCountDescCreatedAtDesc()).contains(tag);

        assertThat(postCategoryRepository.findByPostId(1L)).contains(postCategory);
        assertThat(postCategoryRepository.findByPostIdAndCategoryId(1L, category.getCategoryId())).contains(postCategory);
        postCategoryRepository.deleteByPostIdAndCategoryId(1L, category.getCategoryId());
        assertThat(postCategoryRepository.findByPostIdAndCategoryId(1L, category.getCategoryId())).isEmpty();

        assertThat(postTagRepository.findByPostId(1L)).contains(postTag);
        assertThat(postTagRepository.findByPostIdAndTagId(1L, tag.getTagId())).contains(postTag);
        postTagRepository.deleteByPostIdAndTagId(1L, tag.getTagId());
        assertThat(postTagRepository.findByPostIdAndTagId(1L, tag.getTagId())).isEmpty();
    }

    private Category category(String name, String slug) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .description("desc")
                .postCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Tag tag(String name, String slug) {
        return Tag.builder()
                .name(name)
                .slug(slug)
                .postCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

