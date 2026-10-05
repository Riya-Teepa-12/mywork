package com.app.newsletterservice.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscribeRequest {
    @NotBlank
    @Email
    private String email;
    private Long userId;
    private String fullName;
    private List<String> preferences;
}
