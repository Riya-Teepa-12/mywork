package com.app.categoryservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private Long categoryId;
    private String name;
    private String slug;
    private String description;
    private Long parentCategoryId;
    private Long postCount;
    private LocalDateTime createdAt;
}
