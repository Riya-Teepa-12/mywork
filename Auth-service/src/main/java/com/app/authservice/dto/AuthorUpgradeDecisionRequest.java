package com.app.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthorUpgradeDecisionRequest {
    private boolean approve;

    @Size(max = 1000)
    private String reason;
}
