package com.app.categoryservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostTaxonomyRequest {
    @NotNull
    private Long postId;
    @NotNull
    private Long taxonomyId;
}
