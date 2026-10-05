package com.app.categoryservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TagResponse {
    private Long tagId;
    private String name;
    private String slug;
    private Long postCount;
    private LocalDateTime createdAt;
}
