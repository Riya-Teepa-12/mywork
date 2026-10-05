package com.app.authservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.authservice.dto.AuditLogRequest;
import com.app.authservice.dto.AuditLogResponse;
import com.app.authservice.dto.AuthResponse;
import com.app.authservice.dto.AuthorUpgradeDecisionRequest;
import com.app.authservice.dto.AuthorUpgradeRequestResponse;
import com.app.authservice.dto.BecomeAuthorRequest;
import com.app.authservice.dto.ChangePasswordRequest;
import com.app.authservice.dto.ChangeRoleRequest;
import com.app.authservice.dto.ForgotPasswordOtpRequest;
import com.app.authservice.dto.LoginRequest;
import com.app.authservice.dto.OAuthLoginRequest;
import com.app.authservice.dto.ProfileUpdateRequest;
import com.app.authservice.dto.PublicUserProfileResponse;
import com.app.authservice.dto.RefreshTokenRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordWithOtpRequest;
import com.app.authservice.dto.SimpleApiResponse;
import com.app.authservice.dto.TokenValidationResponse;
import com.app.authservice.dto.UserProfileResponse;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/request-otp")
    public SimpleApiResponse requestSignupOtp(@Valid @RequestBody RegisterRequest request) {
        authService.requestSignupOtp(request);
        return new SimpleApiResponse("OTP sent to your email");
    }

    @PostMapping("/register/verify-otp")
    public AuthResponse verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest request) {
        return authService.verifySignupOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth/login")
    public AuthResponse oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return authService.oauthLogin(request);
    }

    @PostMapping("/password/forgot/request-otp")
    public SimpleApiResponse requestForgotPasswordOtp(@Valid @RequestBody ForgotPasswordOtpRequest request) {
        authService.requestPasswordResetOtp(request);
        return new SimpleApiResponse("If this email exists, an OTP has been sent");
    }

    @PostMapping("/password/forgot/reset")
    public SimpleApiResponse resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        authService.resetPasswordWithOtp(request);
        return new SimpleApiResponse("Password reset successful");
    }

    @PostMapping("/logout")
    public SimpleApiResponse logout() {
        authService.logout();
        return new SimpleApiResponse("Logged out");
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @GetMapping("/validate")
    public TokenValidationResponse validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        String token = tokenParam;
        if (token == null && authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }
        if (token == null || token.isBlank()) {
            return TokenValidationResponse.builder().valid(false).build();
        }
        return authService.validateToken(token);
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(Authentication authentication) {
        return authService.getUserByEmail(authentication.getName());
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/profile/become-author")
    public AuthorUpgradeRequestResponse becomeAuthor(Authentication authentication,
            @Valid @RequestBody BecomeAuthorRequest request) {
        return authService.becomeAuthor(authentication.getName(), request);
    }

    @GetMapping("/profile/author-request")
    public AuthorUpgradeRequestResponse getMyAuthorRequest(Authentication authentication) {
        return authService.getMyAuthorUpgradeRequest(authentication.getName());
    }

    @PutMapping("/password")
    public SimpleApiResponse changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return new SimpleApiResponse("Password updated");
    }

    @PatchMapping("/deactivate")
    public SimpleApiResponse deactivateAccount(Authentication authentication) {
        authService.deactivateAccount(authentication.getName());
        return new SimpleApiResponse("Account deactivated");
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserProfileResponse> searchUsers(@RequestParam(value = "q", defaultValue = "") String keyword) {
        return authService.searchUsers(keyword);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserProfileResponse> getUsers(
            @RequestParam(value = "q", defaultValue = "") String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "active", required = false) Boolean active) {
        return authService.getUsers(keyword, role, active);
    }

    @GetMapping("/users/count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> getUserCount(
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "active", required = false) Boolean active) {
        return Map.of("count", authService.getUserCount(role, active));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse getUserById(@PathVariable Long userId) {
        return authService.getUserById(userId);
    }

    @GetMapping("/public/users/{userId}")
    public PublicUserProfileResponse getPublicUserById(@PathVariable Long userId) {
        return authService.getPublicUserById(userId);
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse updateRole(@PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest request) {
        return authService.updateRole(userId, request);
    }

    @PutMapping("/users/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse suspendUser(@PathVariable Long userId) {
        return authService.setUserActive(userId, false);
    }

    @PutMapping("/users/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse reactivateUser(@PathVariable Long userId) {
        return authService.setUserActive(userId, true);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public SimpleApiResponse deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return new SimpleApiResponse("User deleted");
    }

    @PostMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public AuditLogResponse recordAudit(Authentication authentication, @Valid @RequestBody AuditLogRequest request) {
        return authService.recordAudit(authentication.getName(), request);
    }

    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogResponse> getAuditLogs(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return authService.getAuditLogs(limit);
    }

    @GetMapping("/admin/author-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuthorUpgradeRequestResponse> getAuthorRequests(
            @RequestParam(value = "status", required = false) AuthorUpgradeStatus status) {
        return authService.getAuthorUpgradeRequests(status);
    }

    @GetMapping("/admin/author-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorUpgradeRequestResponse getAuthorRequestById(@PathVariable Long requestId) {
        return authService.getAuthorUpgradeRequestById(requestId);
    }

    @PutMapping("/admin/author-requests/{requestId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorUpgradeRequestResponse decideAuthorRequest(
            Authentication authentication,
            @PathVariable Long requestId,
            @Valid @RequestBody AuthorUpgradeDecisionRequest request) {
        return authService.decideAuthorUpgradeRequest(authentication.getName(), requestId, request);
    }
}
