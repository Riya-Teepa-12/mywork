package com.app.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentUpdateRequest {
    @NotNull
    private Long authorId;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
