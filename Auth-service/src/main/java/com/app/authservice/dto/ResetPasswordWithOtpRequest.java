package com.app.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordWithOtpRequest {
    @NotBlank
    @Email
    @Pattern(regexp = ValidationPatterns.EMAIL_REGEX, message = "must be a valid email address")
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    @Size(min = 8, max = 120)
    @Pattern(
            regexp = ValidationPatterns.STRONG_CREDENTIAL_REGEX,
            message = "must contain at least 1 uppercase letter, 1 number, and 1 special character")
    private String newPassword;
}
