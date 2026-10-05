package com.app.categoryservice.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.app.categoryservice.dto.CategoryRequest;
import com.app.categoryservice.dto.PostTaxonomyRequest;
import com.app.categoryservice.dto.TagRequest;
import com.app.categoryservice.dto.TagResponse;
import com.app.categoryservice.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryResourceTest {

    @Mock
    private CategoryService categoryService;

    @Test
    void createCategoryRequiresAuthorOrAdmin() {
        CategoryResource resource = new CategoryResource(categoryService);
        CategoryRequest request = new CategoryRequest();
        request.setName("Technology");

        assertThatThrownBy(() -> resource.createCategory(request, "READER"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createAndDeleteTagDelegateToService() {
        CategoryResource resource = new CategoryResource(categoryService);
        TagRequest request = new TagRequest();
        request.setName("Java");
        when(categoryService.createTag(request)).thenReturn(TagResponse.builder().tagId(1L).build());

        resource.createTag(request, "AUTHOR");
        resource.deleteTag(1L, "ADMIN");

        verify(categoryService).createTag(request);
        verify(categoryService).deleteTag(1L);
    }

    @Test
    void taxonomyEndpointsDelegateToService() {
        CategoryResource resource = new CategoryResource(categoryService);
        PostTaxonomyRequest request = new PostTaxonomyRequest();
        request.setPostId(1L);
        request.setTaxonomyId(2L);
        when(categoryService.getCategoriesByPost(1L)).thenReturn(List.of());
        when(categoryService.getTagsByPost(1L)).thenReturn(List.of());

        resource.addCategoryToPost(request, "ADMIN");
        resource.removeTagFromPost(request, "AUTHOR");
        resource.getCategoriesByPost(1L);
        resource.getTagsByPost(1L);

        verify(categoryService).addCategoryToPost(request);
        verify(categoryService).removeTagFromPost(request);
    }
}


