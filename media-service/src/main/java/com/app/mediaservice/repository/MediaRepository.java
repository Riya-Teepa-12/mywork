package com.app.mediaservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.mediaservice.entity.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUploaderId(Long uploaderId);
    Optional<Media> findByMediaId(Long mediaId);
    List<Media> findByLinkedPostId(Long linkedPostId);
    List<Media> findByMimeType(String mimeType);
    List<Media> findByIsDeleted(Boolean isDeleted);
    long countByUploaderId(Long uploaderId);
    void deleteByMediaId(Long mediaId);
}
