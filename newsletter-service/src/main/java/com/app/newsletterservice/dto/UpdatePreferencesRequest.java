package com.app.newsletterservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePreferencesRequest {
    @NotBlank
    private String email;
    private List<String> preferences;
}
