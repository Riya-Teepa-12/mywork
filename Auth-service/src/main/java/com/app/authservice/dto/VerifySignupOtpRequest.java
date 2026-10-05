package com.app.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifySignupOtpRequest {
    @NotBlank
    @Email
    @Pattern(regexp = ValidationPatterns.EMAIL_REGEX, message = "must be a valid email address")
    private String email;

    @NotBlank
    private String otp;
}
