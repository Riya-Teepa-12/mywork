package com.app.authservice.dto;

import com.app.authservice.entity.Role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotNull
    private Role role;
}
