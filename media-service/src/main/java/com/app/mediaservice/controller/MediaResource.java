package com.app.mediaservice.controller;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.app.mediaservice.dto.LinkPostRequest;
import com.app.mediaservice.dto.MediaResponse;
import com.app.mediaservice.dto.UpdateAltTextRequest;
import com.app.mediaservice.service.MediaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaResource {

    private final MediaService mediaService;

    @PostMapping
    public MediaResponse uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") Long uploaderId,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAuthenticated(actorId);
        if (!isAdmin(actorRole) && !actorId.equals(uploaderId)) {
            throw forbidden("You can upload media only for your own account");
        }
        return mediaService.uploadMedia(file, uploaderId, altText);
    }

    @GetMapping("/{mediaId}")
    public MediaResponse getById(@PathVariable Long mediaId) {
        return mediaService.getMediaById(mediaId);
    }

    @GetMapping("/uploader/{uploaderId}")
    public List<MediaResponse> getByUploader(@PathVariable Long uploaderId) {
        return mediaService.getMediaByUploader(uploaderId);
    }

    @GetMapping("/post/{postId}")
    public List<MediaResponse> getByPost(@PathVariable Long postId) {
        return mediaService.getMediaByPost(postId);
    }

    @GetMapping("/all")
    public List<MediaResponse> getAll(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return mediaService.getAllMedia(includeDeleted);
    }

    @PutMapping("/{mediaId}/alt-text")
    public MediaResponse updateAltText(
            @PathVariable Long mediaId,
            @Valid @RequestBody UpdateAltTextRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireMediaOwnerOrAdmin(mediaId, actorId, actorRole);
        return mediaService.updateAltText(mediaId, request);
    }

    @PostMapping("/link")
    public MediaResponse linkToPost(
            @Valid @RequestBody LinkPostRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireMediaOwnerOrAdmin(request.getMediaId(), actorId, actorRole);
        return mediaService.linkToPost(request);
    }

    @PostMapping("/{mediaId}/unlink")
    public MediaResponse unlinkFromPost(
            @PathVariable Long mediaId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireMediaOwnerOrAdmin(mediaId, actorId, actorRole);
        return mediaService.unlinkFromPost(mediaId);
    }

    @DeleteMapping("/{mediaId}")
    public Map<String, String> deleteMedia(
            @PathVariable Long mediaId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireMediaOwnerOrAdmin(mediaId, actorId, actorRole);
        mediaService.deleteMedia(mediaId);
        return Map.of("message", "Media deleted");
    }

    @DeleteMapping("/cleanup")
    public Map<String, Long> cleanupDeleted(@RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        requireAdmin(actorRole);
        return Map.of("deletedCount", mediaService.cleanupDeleted());
    }

    private void requireMediaOwnerOrAdmin(Long mediaId, Long actorId, String actorRole) {
        requireAuthenticated(actorId);
        if (isAdmin(actorRole)) {
            return;
        }
        MediaResponse media = mediaService.getMediaById(mediaId);
        if (media.getUploaderId() == null || !media.getUploaderId().equals(actorId)) {
            throw forbidden("Only media owner can perform this action");
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

    private void requireAdmin(String actorRole) {
        if (!isAdmin(actorRole)) {
            throw forbidden("Admin role is required");
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
