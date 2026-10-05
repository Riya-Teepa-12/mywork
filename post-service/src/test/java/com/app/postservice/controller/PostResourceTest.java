package com.app.postservice.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.app.postservice.dto.PostCreateRequest;
import com.app.postservice.dto.PostResponse;
import com.app.postservice.dto.PostUpdateRequest;
import com.app.postservice.entity.PostStatus;
import com.app.postservice.service.PostService;

@ExtendWith(MockitoExtension.class)
class PostResourceTest {

    @Mock
    private PostService postService;

    @Test
    void createPostAllowsOwnerAndAdmin() {
        PostCreateRequest request = new PostCreateRequest();
        request.setAuthorId(11L);
        request.setTitle("Hello World");
        request.setContent("content");
        when(postService.createPost(any(), any())).thenReturn(PostResponse.builder().postId(1L).build());

        PostResource resource = new PostResource(postService);
        PostResponse response = resource.createPost(request, 11L, "AUTHOR");

        verify(postService).createPost(request, "AUTHOR");
        org.assertj.core.api.Assertions.assertThat(response.getPostId()).isEqualTo(1L);
    }

    @Test
    void createPostRejectsDifferentUser() {
        PostCreateRequest request = new PostCreateRequest();
        request.setAuthorId(11L);
        request.setTitle("Hello World");
        request.setContent("content");

        PostResource resource = new PostResource(postService);

        assertThatThrownBy(() -> resource.createPost(request, 99L, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getPostsByAuthorUsesPublishedBranchForOtherUsers() {
        PostResource resource = new PostResource(postService);
        when(postService.getPublishedPostsByAuthor(7L)).thenReturn(List.of());

        resource.getPostsByAuthor(7L, 2L, "READER");

        verify(postService).getPublishedPostsByAuthor(7L);
    }

    @Test
    void deletePostRequiresOwnerOrAdmin() {
        PostResource resource = new PostResource(postService);
        when(postService.getPostById(5L)).thenReturn(PostResponse.builder().authorId(9L).build());

        assertThatThrownBy(() -> resource.deletePost(5L, 1L, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void likeAndUnlikeUseAuthenticatedUserId() {
        PostResource resource = new PostResource(postService);

        resource.likePost(10L, 22L, 22L);
        resource.unlikePost(10L, 22L, 22L);

        verify(postService).likePost(10L, 22L);
        verify(postService).unlikePost(10L, 22L);
    }

    @Test
    void allRemainingRoutesDelegateToService() {
        PostResource resource = new PostResource(postService);
        PostResponse ownedPost = PostResponse.builder()
                .postId(5L)
                .authorId(9L)
                .status(PostStatus.DRAFT)
                .build();
        PostUpdateRequest updateRequest = new PostUpdateRequest();
        updateRequest.setTitle("Updated");
        updateRequest.setContent("content");
        updateRequest.setStatus(PostStatus.PUBLISHED);
        when(postService.getPostById(5L)).thenReturn(ownedPost);
        when(postService.getPostBySlug("slug")).thenReturn(ownedPost);
        when(postService.getPostsByAuthor(9L)).thenReturn(List.of(ownedPost));
        when(postService.getPublishedPostsByAuthor(9L)).thenReturn(List.of(ownedPost));
        when(postService.getFollowedAuthorIds(9L)).thenReturn(List.of(10L, 11L));
        when(postService.followAuthor(10L, 9L)).thenReturn(true);
        when(postService.unfollowAuthor(10L, 9L)).thenReturn(true);
        when(postService.getAuthorFollowerCount(10L)).thenReturn(12L);
        when(postService.isFollowingAuthor(10L, 9L)).thenReturn(true);
        when(postService.getAllPosts()).thenReturn(List.of(ownedPost));
        when(postService.getPublishedPosts()).thenReturn(List.of(ownedPost));
        when(postService.getMostViewedPosts(3)).thenReturn(List.of(ownedPost));
        when(postService.searchPosts("inkwell")).thenReturn(List.of(ownedPost));
        when(postService.updatePost(5L, updateRequest)).thenReturn(ownedPost);
        when(postService.publishPost(5L)).thenReturn(ownedPost);
        when(postService.unpublishPost(5L)).thenReturn(ownedPost);
        when(postService.featurePost(5L, true)).thenReturn(ownedPost);
        when(postService.getPostCount(9L)).thenReturn(4L);

        resource.getPostById(5L);
        resource.getPostBySlug("slug");
        resource.getPostsByAuthor(9L, 9L, "AUTHOR");
        resource.getPublishedPostsByAuthor(9L);
        resource.getFollowedAuthors(9L, 9L, "AUTHOR");
        resource.followAuthor(10L, 9L, 9L, "AUTHOR");
        resource.unfollowAuthor(10L, 9L, 9L, "AUTHOR");
        resource.getAuthorFollowerCount(10L);
        resource.isFollowingAuthor(10L, null, 9L, "AUTHOR");
        resource.getAllPosts("ADMIN");
        resource.getPublishedPosts();
        resource.getMostViewedPosts(3, "ADMIN");
        resource.searchPosts("inkwell");
        resource.updatePost(5L, updateRequest, 9L, "AUTHOR");
        resource.publishPost(5L, 9L, "AUTHOR");
        resource.unpublishPost(5L, 9L, "AUTHOR");
        resource.featurePost(5L, true, "ADMIN");
        resource.incrementViews(5L, "session-1");
        resource.deletePost(5L, 9L, "AUTHOR");
        resource.getPostCount(9L);

        verify(postService, atLeastOnce()).getPostById(5L);
        verify(postService).getPostBySlug("slug");
        verify(postService).getPostsByAuthor(9L);
        verify(postService).getFollowedAuthorIds(9L);
        verify(postService).followAuthor(10L, 9L);
        verify(postService).unfollowAuthor(10L, 9L);
        verify(postService).getAllPosts();
        verify(postService).searchPosts("inkwell");
        verify(postService).updatePost(5L, updateRequest);
        verify(postService).publishPost(5L);
        verify(postService).unpublishPost(5L);
        verify(postService).featurePost(5L, true);
        verify(postService).incrementViews(5L, "session-1");
        verify(postService).deletePost(5L);
    }
}

