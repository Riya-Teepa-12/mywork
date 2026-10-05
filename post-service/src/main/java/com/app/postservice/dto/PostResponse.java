package com.app.postservice.dto;

import java.time.LocalDateTime;

import com.app.postservice.entity.PostStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostResponse {
    private Long postId;
    private Long authorId;
    private String authorName;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String featuredImageUrl;
    private PostStatus status;
    private Integer readTimeMin;
    private Long viewCount;
    private Long likesCount;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
