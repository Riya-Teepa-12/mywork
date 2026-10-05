package com.app.postservice.dto;

import com.app.postservice.entity.PostStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostUpdateRequest {
    @NotBlank
    @Size(min = 3, max = 180)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 400)
    private String excerpt;

    @Size(max = 1024)
    private String featuredImageUrl;

    private PostStatus status;
}
