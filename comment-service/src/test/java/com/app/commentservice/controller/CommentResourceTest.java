package com.app.commentservice.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.app.commentservice.dto.CommentCreateRequest;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.CommentUpdateRequest;
import com.app.commentservice.dto.ModerationModeRequest;
import com.app.commentservice.entity.CommentStatus;
import com.app.commentservice.service.CommentService;

@ExtendWith(MockitoExtension.class)
class CommentResourceTest {

    @Mock
    private CommentService commentService;

    @Test
    void addCommentRequiresAuthenticatedSelf() {
        CommentResource resource = new CommentResource(commentService);
        CommentCreateRequest request = new CommentCreateRequest();
        request.setAuthorId(10L);

        assertThatThrownBy(() -> resource.addComment(request, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getAllCommentsRequiresAdmin() {
        CommentResource resource = new CommentResource(commentService);

        assertThatThrownBy(() -> resource.getAllComments(null, "AUTHOR"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateAndDeleteDelegateToService() {
        CommentResource resource = new CommentResource(commentService);
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setAuthorId(10L);
        request.setContent("Updated");
        when(commentService.updateComment(1L, request)).thenReturn(CommentResponse.builder().commentId(1L).build());

        resource.updateComment(1L, request, 10L);
        resource.deleteComment(1L, null, 10L, "AUTHOR");

        verify(commentService).updateComment(1L, request);
        verify(commentService).deleteComment(1L, 10L, "AUTHOR");
    }

    @Test
    void moderationEndpointDelegatesToService() {
        CommentResource resource = new CommentResource(commentService);
        when(commentService.getModerationMode()).thenReturn(true);

        ModerationModeRequest request = new ModerationModeRequest();
        request.setModerationRequired(true);
        resource.setModerationMode(request, "ADMIN");

        verify(commentService).updateModerationMode(any(ModerationModeRequest.class));
    }

    @Test
    void remainingRoutesDelegateToService() {
        CommentResource resource = new CommentResource(commentService);
        ReflectionTestUtils.setField(resource, "configuredInternalApiKey", "secret");
        CommentResponse response = CommentResponse.builder().commentId(1L).authorId(10L).build();
        when(commentService.getCommentsByPost(5L)).thenReturn(java.util.List.of(response));
        when(commentService.getAllComments(CommentStatus.APPROVED)).thenReturn(java.util.List.of(response));
        when(commentService.getCommentById(1L)).thenReturn(response);
        when(commentService.getReplies(1L)).thenReturn(java.util.List.of(response));
        when(commentService.approveComment(1L, 10L, "ADMIN")).thenReturn(response);
        when(commentService.rejectComment(1L, 10L, "ADMIN")).thenReturn(response);
        when(commentService.getCommentCount(5L)).thenReturn(3L);
        when(commentService.getModerationMode()).thenReturn(true);

        resource.getCommentsByPost(5L);
        resource.getAllComments(CommentStatus.APPROVED, "ADMIN");
        resource.getCommentById(1L);
        resource.getReplies(1L);
        resource.deleteCommentsByPost(5L, "secret");
        resource.approveComment(1L, 10L, "ADMIN");
        resource.rejectComment(1L, 10L, "ADMIN");
        resource.likeComment(1L, 10L, 10L);
        resource.unlikeComment(1L, 10L, 10L);
        resource.getCommentCount(5L);
        resource.getModerationMode();
        resource.setModerationMode(new ModerationModeRequest(), "ADMIN");

        verify(commentService).getCommentsByPost(5L);
        verify(commentService).getAllComments(CommentStatus.APPROVED);
        verify(commentService).getCommentById(1L);
        verify(commentService).getReplies(1L);
        verify(commentService).deleteCommentsByPost(5L);
        verify(commentService).approveComment(1L, 10L, "ADMIN");
        verify(commentService).rejectComment(1L, 10L, "ADMIN");
        verify(commentService).likeComment(1L, 10L);
        verify(commentService).unlikeComment(1L, 10L);
    }
}


