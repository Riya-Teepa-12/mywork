package com.app.authservice.dto;

import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String bio;
    private String avatarUrl;
    private AuthProvider provider;
    private boolean active;
}
