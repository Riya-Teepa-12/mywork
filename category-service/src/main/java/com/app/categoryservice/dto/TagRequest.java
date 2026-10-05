package com.app.categoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {
    @NotBlank
    @Size(min = 2, max = 120)
    private String name;
}
