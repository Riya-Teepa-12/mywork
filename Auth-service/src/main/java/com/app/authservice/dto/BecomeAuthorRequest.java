package com.app.authservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BecomeAuthorRequest {

    @NotBlank
    @Size(min = 120, max = 2000)
    private String bio;

    @NotBlank
    @Size(min = 80, max = 2000)
    private String motivation;

    @NotEmpty
    @Size(max = 6)
    private List<@NotBlank @Size(max = 64) String> expertiseCategories;

    @NotEmpty
    @Size(max = 3)
    private List<@NotBlank @Size(max = 512) @Pattern(regexp = "^https?://.+", message = "must be a valid http/https URL") String> writingSampleUrls;
}
