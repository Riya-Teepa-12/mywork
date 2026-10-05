package com.app.postservice.service;

import java.util.List;

import com.app.postservice.dto.PostCreateRequest;
import com.app.postservice.dto.PostResponse;
import com.app.postservice.dto.PostUpdateRequest;

public interface PostService {
    PostResponse createPost(PostCreateRequest request, String actorRole);
    PostResponse getPostById(Long postId);
    PostResponse getPostBySlug(String slug);
    List<PostResponse> getPostsByAuthor(Long authorId);
    List<PostResponse> getPublishedPostsByAuthor(Long authorId);
    List<PostResponse> getAllPosts();
    List<PostResponse> getPublishedPosts();
    List<PostResponse> getMostViewedPosts(int limit);
    List<PostResponse> searchPosts(String keyword);
    PostResponse updatePost(Long postId, PostUpdateRequest request);
    PostResponse publishPost(Long postId);
    PostResponse unpublishPost(Long postId);
    void deletePost(Long postId);
    void incrementViews(Long postId, String sessionId);
    void likePost(Long postId, Long userId);
    void unlikePost(Long postId, Long userId);
    long getPostCount(Long authorId);
    PostResponse featurePost(Long postId, boolean featured);
    boolean followAuthor(Long authorId, Long followerId);
    boolean unfollowAuthor(Long authorId, Long followerId);
    boolean isFollowingAuthor(Long authorId, Long followerId);
    long getAuthorFollowerCount(Long authorId);
    List<Long> getFollowedAuthorIds(Long followerId);
}
