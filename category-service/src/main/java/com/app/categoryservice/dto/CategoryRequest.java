package com.app.categoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank
    @Size(min = 2, max = 120)
    private String name;

    @Size(max = 600)
    private String description;

    private Long parentCategoryId;
}
