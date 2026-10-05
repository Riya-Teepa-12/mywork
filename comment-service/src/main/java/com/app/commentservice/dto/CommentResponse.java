package com.app.commentservice.dto;

import java.time.LocalDateTime;

import com.app.commentservice.entity.CommentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private String authorName;
    private Long parentCommentId;
    private String content;
    private Long likesCount;
    private CommentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
