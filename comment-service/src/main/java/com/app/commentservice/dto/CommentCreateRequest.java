package com.app.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotNull
    private Long postId;

    @NotNull
    private Long authorId;

    @Size(max = 120)
    private String authorName;

    private Long parentCommentId;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
