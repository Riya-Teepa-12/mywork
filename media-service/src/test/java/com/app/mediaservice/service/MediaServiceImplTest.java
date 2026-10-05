package com.app.mediaservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.mediaservice.dto.LinkPostRequest;
import com.app.mediaservice.dto.MediaResponse;
import com.app.mediaservice.dto.UpdateAltTextRequest;
import com.app.mediaservice.entity.Media;
import com.app.mediaservice.repository.MediaRepository;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "mediaBaseUrl", "http://media.local");
        ReflectionTestUtils.setField(mediaService, "mediaStorage", "local");
    }

    @Test
    void uploadMediaPersistsLocalFileMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", new byte[] {1, 2, 3});
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setMediaId(100L);
            media.setUploadedAt(LocalDateTime.now());
            return media;
        });

        MediaResponse response = mediaService.uploadMedia(file, 7L, "A caption");

        assertThat(response.getMediaId()).isEqualTo(100L);
        assertThat(response.getUploaderId()).isEqualTo(7L);
        assertThat(response.getUrl()).startsWith("http://media.local/");
    }

    @Test
    void updateLinkAndCleanupDelegateToRepository() {
        Media media = Media.builder()
                .mediaId(1L)
                .uploaderId(7L)
                .filename("sample.png")
                .originalName("sample.png")
                .url("http://media.local/sample.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .altText("alt")
                .linkedPostId(null)
                .uploadedAt(LocalDateTime.now())
                .isDeleted(true)
                .build();
        when(mediaRepository.findByMediaId(1L)).thenReturn(java.util.Optional.of(media));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.findByIsDeleted(true)).thenReturn(java.util.List.of(media));

        UpdateAltTextRequest altTextRequest = new UpdateAltTextRequest();
        altTextRequest.setAltText("Updated alt");
        LinkPostRequest linkPostRequest = new LinkPostRequest();
        linkPostRequest.setMediaId(1L);
        linkPostRequest.setPostId(99L);

        assertThat(mediaService.updateAltText(1L, altTextRequest).getAltText()).isEqualTo("Updated alt");
        assertThat(mediaService.linkToPost(linkPostRequest).getLinkedPostId()).isEqualTo(99L);
        assertThat(mediaService.unlinkFromPost(1L).getLinkedPostId()).isNull();
        assertThat(mediaService.cleanupDeleted()).isEqualTo(1L);

        verify(mediaRepository).deleteAll(java.util.List.of(media));
    }

    @Test
    void getAllMediaReturnsOnlyNonDeletedItems() {
        Media active = Media.builder()
                .mediaId(2L)
                .uploaderId(8L)
                .filename("active.png")
                .originalName("active.png")
                .url("http://media.local/active.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .altText("alt")
                .linkedPostId(null)
                .uploadedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        when(mediaRepository.findByIsDeleted(false)).thenReturn(java.util.List.of(active));

        assertThat(mediaService.getAllMedia(true)).hasSize(1);
        assertThat(mediaService.getMediaByUploader(8L)).isEmpty();
    }

    @Test
    void uploadValidationAndLookupBranchesAreCovered() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> mediaService.uploadMedia(empty, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is required");

        MockMultipartFile unsupported = new MockMultipartFile("file", "x.txt", "text/plain", new byte[] {1});
        assertThatThrownBy(() -> mediaService.uploadMedia(unsupported, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");

        Media deleted = Media.builder()
                .mediaId(3L)
                .uploaderId(9L)
                .filename("d.png")
                .originalName("d.png")
                .url("http://media.local/d.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .isDeleted(true)
                .uploadedAt(LocalDateTime.now())
                .build();
        when(mediaRepository.findByMediaId(3L)).thenReturn(java.util.Optional.of(deleted));
        assertThatThrownBy(() -> mediaService.getMediaById(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void repositoryReadAndDeleteBranchesAreCovered() {
        Media active = Media.builder()
                .mediaId(4L)
                .uploaderId(10L)
                .filename("active.png")
                .originalName("active.png")
                .url("http://media.local/active.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .linkedPostId(77L)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();
        when(mediaRepository.findByMediaId(4L)).thenReturn(java.util.Optional.of(active));
        when(mediaRepository.findByUploaderId(10L)).thenReturn(java.util.List.of(active));
        when(mediaRepository.findByLinkedPostId(77L)).thenReturn(java.util.List.of(active));

        assertThat(mediaService.getMediaByUploader(10L)).hasSize(1);
        assertThat(mediaService.getMediaByPost(77L)).hasSize(1);
        mediaService.deleteMedia(4L);
        verify(mediaRepository).delete(active);
    }

    @Test
    void privateHelperMethodsCoverS3UrlAndPrefixBranches() {
        ReflectionTestUtils.setField(mediaService, "s3Region", "ap-south-1");
        ReflectionTestUtils.setField(mediaService, "s3Bucket", "bucket");
        ReflectionTestUtils.setField(mediaService, "s3Endpoint", "http://minio:9000/");
        ReflectionTestUtils.setField(mediaService, "s3PublicBaseUrl", "");
        ReflectionTestUtils.setField(mediaService, "s3Prefix", "/media/");

        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "extension", "a.PNG")).isEqualTo(".png");
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "normalizePrefix", "/a/b/")).isEqualTo("a/b");
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "trimTrailingSlash", "http://x///"))
                .isEqualTo("http://x");
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "buildS3ObjectKey", "x.png"))
                .isEqualTo("media/x.png");
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "buildS3PublicUrl", "media/x.png"))
                .isEqualTo("http://minio:9000/bucket/media/x.png");

        ReflectionTestUtils.setField(mediaService, "mediaStorage", "s3");
        ReflectionTestUtils.setField(mediaService, "s3Bucket", "");
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", new byte[] {1});
        assertThatThrownBy(() -> mediaService.uploadMedia(file, 1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bucket");
    }

    @Test
    void additionalHelperAndRepositoryBranchesAreCovered() {
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "extension", "noext")).isEmpty();
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "normalizePrefix", "///")).isEmpty();
        assertThat((String) ReflectionTestUtils.invokeMethod(mediaService, "buildS3PublicUrl", "k"))
                .contains(".amazonaws.com/");

        ReflectionTestUtils.setField(mediaService, "mediaStorage", "local");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(mediaService, "isS3Storage")).isFalse();

        when(mediaRepository.findByMediaId(999L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> mediaService.getMediaById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Media not found");

        when(mediaRepository.findByIsDeleted(true)).thenReturn(java.util.List.of());
        assertThat(mediaService.cleanupDeleted()).isZero();
    }

    @Test
    void uploadValidationCoversUploaderAndSizeLimits() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2});
        assertThatThrownBy(() -> mediaService.uploadMedia(file, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploaderId");

        byte[] large = new byte[(int) (11L * 1024L * 1024L)];
        MockMultipartFile huge = new MockMultipartFile("file", "huge.png", "image/png", large);
        assertThatThrownBy(() -> mediaService.uploadMedia(huge, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    void s3UploadDeleteAndCleanupBranchesAreCovered() {
        S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
        ReflectionTestUtils.setField(mediaService, "mediaStorage", "s3");
        ReflectionTestUtils.setField(mediaService, "s3Bucket", "bucket");
        ReflectionTestUtils.setField(mediaService, "s3Client", new AtomicReference<>(s3Client));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setMediaId(77L);
            media.setUploadedAt(LocalDateTime.now());
            return media;
        });
        MediaResponse uploadResponse = mediaService.uploadMedia(file, 5L, "alt");
        assertThat(uploadResponse.getFilename()).isNotBlank();

        Media existing = Media.builder()
                .mediaId(77L)
                .uploaderId(5L)
                .filename("media/a.png")
                .url("http://cdn/media/a.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();
        when(mediaRepository.findByMediaId(77L)).thenReturn(java.util.Optional.of(existing));
        mediaService.deleteMedia(77L);
        verify(s3Client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));

        Media deleted = Media.builder()
                .mediaId(88L)
                .uploaderId(5L)
                .filename("media/deleted.png")
                .isDeleted(true)
                .uploadedAt(LocalDateTime.now())
                .build();
        when(mediaRepository.findByIsDeleted(true)).thenReturn(java.util.List.of(deleted));
        assertThat(mediaService.cleanupDeleted()).isEqualTo(1L);
    }

    @Test
    void s3UploadFailureReturnsIllegalState() {
        S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
        ReflectionTestUtils.setField(mediaService, "mediaStorage", "s3");
        ReflectionTestUtils.setField(mediaService, "s3Bucket", "bucket");
        ReflectionTestUtils.setField(mediaService, "s3Client", new AtomicReference<>(s3Client));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(s3Client)
                .putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        assertThatThrownBy(() -> mediaService.uploadMedia(file, 5L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to upload media to S3");
    }

    @Test
    void getS3ClientBuildsOnceAndCachesClient() {
        ReflectionTestUtils.setField(mediaService, "s3Region", "ap-south-1");
        ReflectionTestUtils.setField(mediaService, "s3Endpoint", "");
        ReflectionTestUtils.setField(mediaService, "s3Client", new AtomicReference<S3Client>());

        S3Client first = (S3Client) ReflectionTestUtils.invokeMethod(mediaService, "getS3Client");
        S3Client second = (S3Client) ReflectionTestUtils.invokeMethod(mediaService, "getS3Client");

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }
}

