package com.app.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 2000)
    private String bio;

    @Size(max = 512)
    private String avatarUrl;
}
