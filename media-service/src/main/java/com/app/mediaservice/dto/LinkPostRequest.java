package com.app.mediaservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkPostRequest {
    @NotNull
    private Long mediaId;
    @NotNull
    private Long postId;
}
