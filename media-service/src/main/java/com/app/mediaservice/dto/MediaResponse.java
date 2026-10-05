package com.app.mediaservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaResponse {
    private Long mediaId;
    private Long uploaderId;
    private String filename;
    private String originalName;
    private String url;
    private String mimeType;
    private Long sizeKb;
    private String altText;
    private Long linkedPostId;
    private LocalDateTime uploadedAt;
    private Boolean deleted;
}
