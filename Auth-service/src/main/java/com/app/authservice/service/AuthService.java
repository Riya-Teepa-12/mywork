package com.app.authservice.service;

import java.util.List;

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
import com.app.authservice.dto.TokenValidationResponse;
import com.app.authservice.dto.UserProfileResponse;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.entity.Role;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    void requestSignupOtp(RegisterRequest request);
    AuthResponse verifySignupOtp(VerifySignupOtpRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse oauthLogin(OAuthLoginRequest request);
    void requestPasswordResetOtp(ForgotPasswordOtpRequest request);
    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);
    void logout();
    TokenValidationResponse validateToken(String token);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserProfileResponse getUserByEmail(String email);
    UserProfileResponse getUserById(Long userId);
    PublicUserProfileResponse getPublicUserById(Long userId);
    UserProfileResponse updateProfile(String currentEmail, ProfileUpdateRequest request);
    AuthorUpgradeRequestResponse becomeAuthor(String currentEmail, BecomeAuthorRequest request);
    AuthorUpgradeRequestResponse getMyAuthorUpgradeRequest(String currentEmail);
    List<AuthorUpgradeRequestResponse> getAuthorUpgradeRequests(AuthorUpgradeStatus status);
    AuthorUpgradeRequestResponse getAuthorUpgradeRequestById(Long requestId);
    AuthorUpgradeRequestResponse decideAuthorUpgradeRequest(
            String adminEmail,
            Long requestId,
            AuthorUpgradeDecisionRequest request);
    void changePassword(String currentEmail, ChangePasswordRequest request);
    List<UserProfileResponse> searchUsers(String keyword);
    List<UserProfileResponse> getUsers(String keyword, Role role, Boolean active);
    long getUserCount(Role role, Boolean active);
    void deactivateAccount(String currentEmail);
    UserProfileResponse setUserActive(Long userId, boolean active);
    void deleteUser(Long userId);
    UserProfileResponse updateRole(Long userId, ChangeRoleRequest request);
    AuditLogResponse recordAudit(String actorEmail, AuditLogRequest request);
    List<AuditLogResponse> getAuditLogs(int limit);
}
