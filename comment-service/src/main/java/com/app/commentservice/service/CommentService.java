package com.app.commentservice.service;

import java.util.List;

import com.app.commentservice.dto.CommentCreateRequest;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.CommentUpdateRequest;
import com.app.commentservice.dto.ModerationModeRequest;
import com.app.commentservice.entity.CommentStatus;

public interface CommentService {
    CommentResponse addComment(CommentCreateRequest request);
    List<CommentResponse> getCommentsByPost(Long postId);
    List<CommentResponse> getAllComments(CommentStatus status);
    CommentResponse getCommentById(Long commentId);
    List<CommentResponse> getReplies(Long commentId);
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request);
    void deleteComment(Long commentId, Long actorId, String actorRole);
    void deleteCommentsByPost(Long postId);
    CommentResponse approveComment(Long commentId, Long actorId, String actorRole);
    CommentResponse rejectComment(Long commentId, Long actorId, String actorRole);
    void likeComment(Long commentId, Long userId);
    void unlikeComment(Long commentId, Long userId);
    long getCommentCount(Long postId);
    boolean getModerationMode();
    void updateModerationMode(ModerationModeRequest request);
}
