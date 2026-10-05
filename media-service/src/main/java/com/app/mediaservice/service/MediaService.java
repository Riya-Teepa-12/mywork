package com.app.mediaservice.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.app.mediaservice.dto.LinkPostRequest;
import com.app.mediaservice.dto.MediaResponse;
import com.app.mediaservice.dto.UpdateAltTextRequest;

public interface MediaService {
    MediaResponse uploadMedia(MultipartFile file, Long uploaderId, String altText);
    MediaResponse getMediaById(Long mediaId);
    List<MediaResponse> getMediaByUploader(Long uploaderId);
    List<MediaResponse> getMediaByPost(Long postId);
    void deleteMedia(Long mediaId);
    MediaResponse updateAltText(Long mediaId, UpdateAltTextRequest request);
    MediaResponse linkToPost(LinkPostRequest request);
    MediaResponse unlinkFromPost(Long mediaId);
    List<MediaResponse> getAllMedia(boolean includeDeleted);
    long cleanupDeleted();
}
