package com.app.commentservice.controller;

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

import com.app.commentservice.dto.CommentCreateRequest;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.CommentUpdateRequest;
import com.app.commentservice.dto.ModerationModeRequest;
import com.app.commentservice.entity.CommentStatus;
import com.app.commentservice.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentResource {

    private final CommentService commentService;

    @PostMapping
    public CommentResponse addComment(
            @Valid @RequestBody CommentCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(request.getAuthorId())) {
            throw forbidden("You can comment only as yourself");
        }
        return commentService.addComment(request);
    }

    @GetMapping("/post/{postId}")
    public List<CommentResponse> getCommentsByPost(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }

    @GetMapping("/all")
    public List<CommentResponse> getAllComments(
            @RequestParam(required = false) CommentStatus status,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return commentService.getAllComments(status);
    }

    @GetMapping("/{commentId}")
    public CommentResponse getCommentById(@PathVariable Long commentId) {
        return commentService.getCommentById(commentId);
    }

    @GetMapping("/{commentId}/replies")
    public List<CommentResponse> getReplies(@PathVariable Long commentId) {
        return commentService.getReplies(commentId);
    }

    @PutMapping("/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(request.getAuthorId())) {
            throw forbidden("You can update only your own comments");
        }
        return commentService.updateComment(commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public Map<String, String> deleteComment(
            @PathVariable Long commentId,
            @RequestParam(required = false) Long actorId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerActorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        Long effectiveActorId = headerActorId != null ? headerActorId : actorId;
        commentService.deleteComment(commentId, effectiveActorId, actorRole);
        return Map.of("message", "Comment deleted");
    }

    @DeleteMapping("/post/{postId}")
    public Map<String, String> deleteCommentsByPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        requireInternalApiKey(internalApiKey);
        commentService.deleteCommentsByPost(postId);
        return Map.of("message", "Comments deleted");
    }

    @PutMapping("/{commentId}/approve")
    public CommentResponse approveComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthenticated(actorId);
        return commentService.approveComment(commentId, actorId, actorRole);
    }

    @PutMapping("/{commentId}/reject")
    public CommentResponse rejectComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthenticated(actorId);
        return commentService.rejectComment(commentId, actorId, actorRole);
    }

    @PutMapping("/{commentId}/like")
    public Map<String, String> likeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(userId)) {
            throw forbidden("You can like only as yourself");
        }
        commentService.likeComment(commentId, userId);
        return Map.of("message", "Comment liked");
    }

    @PutMapping("/{commentId}/unlike")
    public Map<String, String> unlikeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId) {
        requireAuthenticated(actorId);
        if (!actorId.equals(userId)) {
            throw forbidden("You can unlike only as yourself");
        }
        commentService.unlikeComment(commentId, userId);
        return Map.of("message", "Comment unliked");
    }

    @GetMapping("/count")
    public Map<String, Long> getCommentCount(@RequestParam(required = false) Long postId) {
        return Map.of("count", commentService.getCommentCount(postId));
    }

    @GetMapping("/moderation")
    public Map<String, Boolean> getModerationMode() {
        return Map.of("moderationRequired", commentService.getModerationMode());
    }

    @PutMapping("/moderation")
    public Map<String, Boolean> setModerationMode(
            @RequestBody ModerationModeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        commentService.updateModerationMode(request);
        return Map.of("moderationRequired", commentService.getModerationMode());
    }

    @org.springframework.beans.factory.annotation.Value("${inkwell.internal-api-key:ghfyfr7t8hgv7yh}")
    private String configuredInternalApiKey;

    private void requireAuthenticated(Long actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
    }

    private void requireAdmin(String actorRole) {
        if (!"ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole)) {
            throw forbidden("Admin role is required");
        }
    }

    private void requireInternalApiKey(String providedInternalApiKey) {
        if (providedInternalApiKey == null || providedInternalApiKey.isBlank()
                || !providedInternalApiKey.equals(configuredInternalApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
