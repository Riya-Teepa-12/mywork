package com.app.postservice.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.postservice.dto.PostCreateRequest;
import com.app.postservice.dto.PostResponse;
import com.app.postservice.dto.PostUpdateRequest;
import com.app.postservice.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostResource {

    private final PostService postService;

    @PostMapping
    public PostResponse createPost(
            @Valid @RequestBody PostCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthorOrAdmin(actorId, actorRole);
        if (!isAdmin(actorRole) && !request.getAuthorId().equals(actorId)) {
            throw forbidden("You can create posts only for your own account");
        }
        return postService.createPost(request, actorRole);
    }

    @GetMapping("/{postId}")
    public PostResponse getPostById(@PathVariable Long postId) {
        return postService.getPostById(postId);
    }

    @GetMapping("/slug/{slug}")
    public PostResponse getPostBySlug(@PathVariable String slug) {
        return postService.getPostBySlug(slug);
    }

    @GetMapping("/author/{authorId}")
    public List<PostResponse> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        if (isAdmin(actorRole) || (actorId != null && actorId.equals(authorId))) {
            return postService.getPostsByAuthor(authorId);
        }
        return postService.getPublishedPostsByAuthor(authorId);
    }

    @GetMapping("/author/{authorId}/published")
    public List<PostResponse> getPublishedPostsByAuthor(@PathVariable Long authorId) {
        return postService.getPublishedPostsByAuthor(authorId);
    }

    @GetMapping("/follows/{followerId}")
    public List<Long> getFollowedAuthors(
            @PathVariable Long followerId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireSameUserOrAdmin(followerId, actorId, actorRole);
        return postService.getFollowedAuthorIds(followerId);
    }

    @PostMapping("/authors/{authorId}/follow")
    public Map<String, Object> followAuthor(
            @PathVariable Long authorId,
            @RequestParam Long followerId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireSameUserOrAdmin(followerId, actorId, actorRole);
        boolean followed = postService.followAuthor(authorId, followerId);
        return Map.of("followed", followed, "followerCount", postService.getAuthorFollowerCount(authorId));
    }

    @DeleteMapping("/authors/{authorId}/follow")
    public Map<String, Object> unfollowAuthor(
            @PathVariable Long authorId,
            @RequestParam Long followerId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireSameUserOrAdmin(followerId, actorId, actorRole);
        boolean unfollowed = postService.unfollowAuthor(authorId, followerId);
        return Map.of("unfollowed", unfollowed, "followerCount", postService.getAuthorFollowerCount(authorId));
    }

    @GetMapping("/authors/{authorId}/followers/count")
    public Map<String, Long> getAuthorFollowerCount(@PathVariable Long authorId) {
        return Map.of("count", postService.getAuthorFollowerCount(authorId));
    }

    @GetMapping("/authors/{authorId}/followers/check")
    public Map<String, Boolean> isFollowingAuthor(
            @PathVariable Long authorId,
            @RequestParam(required = false) Long followerId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        Long effectiveFollowerId = followerId != null ? followerId : actorId;
        requireSameUserOrAdmin(effectiveFollowerId, actorId, actorRole);
        return Map.of("following", postService.isFollowingAuthor(authorId, effectiveFollowerId));
    }

    @GetMapping("/all")
    public List<PostResponse> getAllPosts(@RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return postService.getAllPosts();
    }

    @GetMapping("/published")
    public List<PostResponse> getPublishedPosts() {
        return postService.getPublishedPosts();
    }

    @GetMapping("/most-viewed")
    public List<PostResponse> getMostViewedPosts(
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return postService.getMostViewedPosts(limit);
    }

    @GetMapping("/search")
    public List<PostResponse> searchPosts(@RequestParam(value = "q", defaultValue = "") String keyword) {
        return postService.searchPosts(keyword);
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requirePostOwnerOrAdmin(postId, actorId, actorRole);
        return postService.updatePost(postId, request);
    }

    @PutMapping("/{postId}/publish")
    public PostResponse publishPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requirePostOwnerOrAdmin(postId, actorId, actorRole);
        return postService.publishPost(postId);
    }

    @PutMapping("/{postId}/unpublish")
    public PostResponse unpublishPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requirePostOwnerOrAdmin(postId, actorId, actorRole);
        return postService.unpublishPost(postId);
    }

    @PutMapping("/{postId}/feature")
    public PostResponse featurePost(
            @PathVariable Long postId,
            @RequestParam boolean featured,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return postService.featurePost(postId, featured);
    }

    @PostMapping("/{postId}/views")
    public Map<String, String> incrementViews(@PathVariable Long postId, @RequestParam String sessionId) {
        postService.incrementViews(postId, sessionId);
        return Map.of("message", "View counted");
    }

    @PostMapping("/{postId}/like")
    public Map<String, String> likePost(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(userId)) {
            throw forbidden("You can only like as yourself");
        }
        postService.likePost(postId, userId);
        return Map.of("message", "Post liked");
    }

    @PostMapping("/{postId}/unlike")
    public Map<String, String> unlikePost(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(userId)) {
            throw forbidden("You can only unlike as yourself");
        }
        postService.unlikePost(postId, userId);
        return Map.of("message", "Post unliked");
    }

    @DeleteMapping("/{postId}")
    public Map<String, String> deletePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requirePostOwnerOrAdmin(postId, actorId, actorRole);
        postService.deletePost(postId);
        return Map.of("message", "Post deleted");
    }

    @GetMapping("/count")
    public Map<String, Long> getPostCount(@RequestParam(value = "authorId", required = false) Long authorId) {
        return Map.of("count", postService.getPostCount(authorId));
    }

    private void requirePostOwnerOrAdmin(Long postId, Long actorId, String actorRole) {
        requireAuthorOrAdmin(actorId, actorRole);
        if (isAdmin(actorRole)) {
            return;
        }
        PostResponse post = postService.getPostById(postId);
        if (post.getAuthorId() == null || !post.getAuthorId().equals(actorId)) {
            throw forbidden("Only the post owner can perform this action");
        }
    }

    private void requireSameUserOrAdmin(Long targetUserId, Long actorId, String actorRole) {
        requireAuthenticated(actorId);
        if (targetUserId == null) {
            throw forbidden("Target user is required");
        }
        if (!isAdmin(actorRole) && !targetUserId.equals(actorId)) {
            throw forbidden("You can perform this action only for your own account");
        }
    }

    private void requireAuthorOrAdmin(Long actorId, String actorRole) {
        requireAuthenticated(actorId);
        if (!isAuthor(actorRole) && !isAdmin(actorRole)) {
            throw forbidden("Author or admin role is required");
        }
    }

    private void requireAdmin(String actorRole) {
        if (!isAdmin(actorRole)) {
            throw forbidden("Admin role is required");
        }
    }

    private void requireAuthenticated(Long actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
    }

    private boolean isAdmin(String actorRole) {
        return "ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole);
    }

    private boolean isAuthor(String actorRole) {
        return "AUTHOR".equalsIgnoreCase(actorRole == null ? "" : actorRole);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
