package com.app.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private String email;
    private String role;
    private Long expiresAt;
}
