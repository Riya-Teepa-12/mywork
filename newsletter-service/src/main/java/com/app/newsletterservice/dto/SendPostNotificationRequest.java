package com.app.newsletterservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendPostNotificationRequest {
    @NotNull
    private Long postId;
    @NotBlank
    private String title;
    @NotBlank
    private String slug;
    private String excerpt;
}
