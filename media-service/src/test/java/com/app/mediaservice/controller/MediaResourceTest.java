package com.app.mediaservice.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.app.mediaservice.dto.LinkPostRequest;
import com.app.mediaservice.dto.MediaResponse;
import com.app.mediaservice.dto.UpdateAltTextRequest;
import com.app.mediaservice.service.MediaService;

@ExtendWith(MockitoExtension.class)
class MediaResourceTest {

    @Mock
    private MediaService mediaService;

    @Test
    void uploadMediaRequiresAuthenticatedOwner() {
        MediaResource resource = new MediaResource(mediaService);
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", new byte[] {1});

        assertThatThrownBy(() -> resource.uploadMedia(file, 10L, null, null, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void ownerCanUpdateLinkAndDeleteMedia() {
        MediaResource resource = new MediaResource(mediaService);
        MediaResponse response = MediaResponse.builder().mediaId(1L).uploaderId(10L).build();
        when(mediaService.getMediaById(1L)).thenReturn(response);
        when(mediaService.updateAltText(any(), any())).thenReturn(response);
        when(mediaService.linkToPost(any())).thenReturn(response);
        when(mediaService.unlinkFromPost(1L)).thenReturn(response);

        UpdateAltTextRequest altTextRequest = new UpdateAltTextRequest();
        altTextRequest.setAltText("alt");
        LinkPostRequest linkPostRequest = new LinkPostRequest();
        linkPostRequest.setMediaId(1L);
        linkPostRequest.setPostId(2L);

        resource.updateAltText(1L, altTextRequest, 10L, "AUTHOR");
        resource.linkToPost(linkPostRequest, 10L, "AUTHOR");
        resource.unlinkFromPost(1L, 10L, "AUTHOR");
        resource.deleteMedia(1L, 10L, "AUTHOR");

        verify(mediaService).updateAltText(1L, altTextRequest);
        verify(mediaService).linkToPost(linkPostRequest);
        verify(mediaService).unlinkFromPost(1L);
        verify(mediaService).deleteMedia(1L);
    }

    @Test
    void adminCanViewAllAndCleanup() {
        MediaResource resource = new MediaResource(mediaService);
        when(mediaService.getAllMedia(true)).thenReturn(List.of());
        when(mediaService.cleanupDeleted()).thenReturn(0L);

        resource.getAll(true, "ADMIN");
        resource.cleanupDeleted("ADMIN");

        verify(mediaService).getAllMedia(true);
        verify(mediaService).cleanupDeleted();
    }

    @Test
    void adminCanUploadAndPublicReadRoutesDelegateToService() {
        MediaResource resource = new MediaResource(mediaService);
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", new byte[] {1});
        MediaResponse response = MediaResponse.builder().mediaId(2L).uploaderId(99L).build();
        when(mediaService.uploadMedia(file, 99L, "alt")).thenReturn(response);
        when(mediaService.getMediaById(2L)).thenReturn(response);
        when(mediaService.getMediaByUploader(99L)).thenReturn(List.of(response));
        when(mediaService.getMediaByPost(7L)).thenReturn(List.of(response));

        resource.uploadMedia(file, 99L, "alt", 1L, "ADMIN");
        resource.getById(2L);
        resource.getByUploader(99L);
        resource.getByPost(7L);

        verify(mediaService).uploadMedia(file, 99L, "alt");
        verify(mediaService).getMediaById(2L);
        verify(mediaService).getMediaByUploader(99L);
        verify(mediaService).getMediaByPost(7L);
    }

    @Test
    void ownerGuardsRejectUnauthorizedAccess() {
        MediaResource resource = new MediaResource(mediaService);
        MediaResponse response = MediaResponse.builder().mediaId(5L).uploaderId(10L).build();
        when(mediaService.getMediaById(5L)).thenReturn(response);

        UpdateAltTextRequest altTextRequest = new UpdateAltTextRequest();
        altTextRequest.setAltText("alt");
        LinkPostRequest linkPostRequest = new LinkPostRequest();
        linkPostRequest.setMediaId(5L);
        linkPostRequest.setPostId(2L);

        assertThatThrownBy(() -> resource.updateAltText(5L, altTextRequest, 11L, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> resource.linkToPost(linkPostRequest, null, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> resource.getAll(false, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }
}

