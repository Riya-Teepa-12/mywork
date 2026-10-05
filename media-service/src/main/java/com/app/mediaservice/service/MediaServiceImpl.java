package com.app.mediaservice.service;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.app.mediaservice.dto.LinkPostRequest;
import com.app.mediaservice.dto.MediaResponse;
import com.app.mediaservice.dto.UpdateAltTextRequest;
import com.app.mediaservice.entity.Media;
import com.app.mediaservice.repository.MediaRepository;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    private final MediaRepository mediaRepository;

    @Value("${inkwell.media.base-url}")
    private String mediaBaseUrl;

    @Value("${inkwell.media.storage:local}")
    private String mediaStorage;

    @Value("${inkwell.media.s3.bucket:}")
    private String s3Bucket;

    @Value("${inkwell.media.s3.region:ap-south-1}")
    private String s3Region;

    @Value("${inkwell.media.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${inkwell.media.s3.public-base-url:}")
    private String s3PublicBaseUrl;

    @Value("${inkwell.media.s3.prefix:media}")
    private String s3Prefix;

    private AtomicReference<S3Client> s3Client = new AtomicReference<>();

    @Override
    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, Long uploaderId, String altText) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (uploaderId == null) {
            throw new IllegalArgumentException("uploaderId is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File exceeds 10MB limit");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String extension = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        String storedFilename = filename;
        String url = trimTrailingSlash(mediaBaseUrl) + "/" + filename;

        if (isS3Storage()) {
            ensureS3Configured();
            String objectKey = buildS3ObjectKey(filename);
            uploadToS3(objectKey, file, type);
            storedFilename = objectKey;
            url = buildS3PublicUrl(objectKey);
        }

        Media media = Media.builder()
                .uploaderId(uploaderId)
                .filename(storedFilename)
                .originalName(file.getOriginalFilename() == null ? filename : file.getOriginalFilename())
                .url(url)
                .mimeType(type)
                .sizeKb(Math.max(1L, file.getSize() / 1024))
                .altText(altText)
                .isDeleted(false)
                .build();

        media = mediaRepository.save(media);
        return toResponse(media);
    }

    @Override
    public MediaResponse getMediaById(Long mediaId) {
        Media media = findMedia(mediaId);
        if (Boolean.TRUE.equals(media.getIsDeleted())) {
            throw new IllegalArgumentException("Media not found");
        }
        return toResponse(media);
    }

    @Override
    public List<MediaResponse> getMediaByUploader(Long uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId).stream()
                .filter(media -> !media.getIsDeleted())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<MediaResponse> getMediaByPost(Long postId) {
        return mediaRepository.findByLinkedPostId(postId).stream()
                .filter(media -> !media.getIsDeleted())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId) {
        Media media = findMedia(mediaId);
        if (isS3Storage() && media.getFilename() != null && !media.getFilename().isBlank()) {
            deleteFromS3Quietly(media.getFilename());
        }
        mediaRepository.delete(media);
    }

    @Override
    @Transactional
    public MediaResponse updateAltText(Long mediaId, UpdateAltTextRequest request) {
        Media media = findMedia(mediaId);
        media.setAltText(request.getAltText());
        media = mediaRepository.save(media);
        return toResponse(media);
    }

    @Override
    @Transactional
    public MediaResponse linkToPost(LinkPostRequest request) {
        Media media = findMedia(request.getMediaId());
        media.setLinkedPostId(request.getPostId());
        media = mediaRepository.save(media);
        return toResponse(media);
    }

    @Override
    @Transactional
    public MediaResponse unlinkFromPost(Long mediaId) {
        Media media = findMedia(mediaId);
        media.setLinkedPostId(null);
        media = mediaRepository.save(media);
        return toResponse(media);
    }

    @Override
    public List<MediaResponse> getAllMedia(boolean includeDeleted) {
        List<Media> mediaList = mediaRepository.findByIsDeleted(false);
        return mediaList.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public long cleanupDeleted() {
        List<Media> deleted = mediaRepository.findByIsDeleted(true);
        if (isS3Storage()) {
            deleted.stream()
                    .map(Media::getFilename)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(this::deleteFromS3Quietly);
        }
        long size = deleted.size();
        mediaRepository.deleteAll(deleted);
        return size;
    }

    private Media findMedia(Long mediaId) {
        return mediaRepository.findByMediaId(mediaId).orElseThrow(() -> new IllegalArgumentException("Media not found"));
    }

    private String extension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot == -1 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot).toLowerCase();
    }

    private boolean isS3Storage() {
        return "s3".equalsIgnoreCase(mediaStorage == null ? "" : mediaStorage.trim());
    }

    private void ensureS3Configured() {
        if (s3Bucket == null || s3Bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket is not configured");
        }
    }

    private String buildS3ObjectKey(String filename) {
        String normalizedPrefix = normalizePrefix(s3Prefix);
        if (normalizedPrefix.isBlank()) {
            return filename;
        }
        return normalizedPrefix + "/" + filename;
    }

    private String normalizePrefix(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void uploadToS3(String objectKey, MultipartFile file, String contentType) {
        try {
            getS3Client().putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (Exception ex) {
            log.error("Unable to upload media object {} to S3 bucket {}", objectKey, s3Bucket, ex);
            throw new IllegalStateException("Unable to upload media to S3");
        }
    }

    private void deleteFromS3Quietly(String objectKey) {
        try {
            getS3Client().deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(objectKey)
                            .build());
        } catch (Exception ignored) {
        }
    }

    private String buildS3PublicUrl(String objectKey) {
        if (s3PublicBaseUrl != null && !s3PublicBaseUrl.isBlank()) {
            return trimTrailingSlash(s3PublicBaseUrl) + "/" + objectKey;
        }
        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            return trimTrailingSlash(s3Endpoint) + "/" + s3Bucket + "/" + objectKey;
        }
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + objectKey;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private S3Client getS3Client() {
        S3Client existing = s3Client.get();
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = s3Client.get();
            if (existing != null) {
                return existing;
            }
            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(s3Region))
                    .credentialsProvider(DefaultCredentialsProvider.create());

            if (s3Endpoint != null && !s3Endpoint.isBlank()) {
                builder = builder
                        .endpointOverride(URI.create(s3Endpoint))
                        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            }
            S3Client created = builder.build();
            s3Client.set(created);
            return created;
        }
    }

    private MediaResponse toResponse(Media media) {
        return MediaResponse.builder()
                .mediaId(media.getMediaId())
                .uploaderId(media.getUploaderId())
                .filename(media.getFilename())
                .originalName(media.getOriginalName())
                .url(media.getUrl())
                .mimeType(media.getMimeType())
                .sizeKb(media.getSizeKb())
                .altText(media.getAltText())
                .linkedPostId(media.getLinkedPostId())
                .uploadedAt(media.getUploadedAt())
                .deleted(media.getIsDeleted())
                .build();
    }
}
