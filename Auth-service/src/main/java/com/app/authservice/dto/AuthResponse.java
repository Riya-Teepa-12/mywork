package com.app.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresAt;
    private UserProfileResponse user;
}
