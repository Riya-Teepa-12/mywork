package com.app.authservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.app.authservice.dto.AuthResponse;
import com.app.authservice.dto.AuthorUpgradeDecisionRequest;
import com.app.authservice.dto.AuthorUpgradeRequestResponse;
import com.app.authservice.dto.BecomeAuthorRequest;
import com.app.authservice.dto.ChangeRoleRequest;
import com.app.authservice.dto.ForgotPasswordOtpRequest;
import com.app.authservice.dto.LoginRequest;
import com.app.authservice.dto.OAuthLoginRequest;
import com.app.authservice.dto.ProfileUpdateRequest;
import com.app.authservice.dto.RefreshTokenRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordWithOtpRequest;
import com.app.authservice.dto.SimpleApiResponse;
import com.app.authservice.dto.TokenValidationResponse;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.dto.UserProfileResponse;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.entity.Role;
import com.app.authservice.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void registerLoginRefreshAndValidateDelegateToService() {
        AuthController controller = new AuthController(authService);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("jane");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Jane Doe");
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("password123");
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setToken("token");

        when(authService.register(registerRequest)).thenReturn(AuthResponse.builder().accessToken("token").build());
        when(authService.login(loginRequest)).thenReturn(AuthResponse.builder().accessToken("token").build());
        when(authService.refreshToken(refreshTokenRequest)).thenReturn(AuthResponse.builder().accessToken("token").build());
        when(authService.validateToken("token")).thenReturn(TokenValidationResponse.builder().valid(true).build());

        assertThat(controller.register(registerRequest).getAccessToken()).isEqualTo("token");
        assertThat(controller.login(loginRequest).getAccessToken()).isEqualTo("token");
        assertThat(controller.refreshToken(refreshTokenRequest).getAccessToken()).isEqualTo("token");
        assertThat(controller.validateToken("Bearer token", null).isValid()).isTrue();

        verify(authService).register(registerRequest);
        verify(authService).login(loginRequest);
        verify(authService).refreshToken(refreshTokenRequest);
        verify(authService).validateToken("token");
    }

    @Test
    void profileAndAdminRoutesDelegateToService() {
        AuthController controller = new AuthController(authService);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("jane@example.com");
        com.app.authservice.dto.ProfileUpdateRequest profileUpdateRequest = new com.app.authservice.dto.ProfileUpdateRequest();
        profileUpdateRequest.setBio("Bio");
        profileUpdateRequest.setAvatarUrl("http://avatar");
        com.app.authservice.dto.ChangePasswordRequest changePasswordRequest = new com.app.authservice.dto.ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("current");
        changePasswordRequest.setNewPassword("newpassword");

        when(authService.getUserByEmail("jane@example.com")).thenReturn(UserProfileResponse.builder().email("jane@example.com").build());
        when(authService.updateProfile("jane@example.com", profileUpdateRequest)).thenReturn(UserProfileResponse.builder().email("jane@example.com").build());
        doNothing().when(authService).changePassword("jane@example.com", changePasswordRequest);
        when(authService.searchUsers("jane")).thenReturn(List.of());

        controller.getProfile(authentication);
        controller.updateProfile(authentication, profileUpdateRequest);
        SimpleApiResponse passwordResponse = controller.changePassword(authentication, changePasswordRequest);
        assertThat(passwordResponse.getMessage()).isEqualTo("Password updated");
        controller.searchUsers("jane");

        verify(authService).getUserByEmail("jane@example.com");
        verify(authService).updateProfile("jane@example.com", profileUpdateRequest);
        verify(authService).changePassword("jane@example.com", changePasswordRequest);
    }

    @Test
    void remainingRoutesDelegateToService() {
        AuthController controller = new AuthController(authService);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@example.com");
        RegisterRequest registerRequest = new RegisterRequest();
        VerifySignupOtpRequest verifySignupOtpRequest = new VerifySignupOtpRequest();
        ForgotPasswordOtpRequest forgotPasswordOtpRequest = new ForgotPasswordOtpRequest();
        ResetPasswordWithOtpRequest resetPasswordWithOtpRequest = new ResetPasswordWithOtpRequest();
        OAuthLoginRequest oauthLoginRequest = new OAuthLoginRequest();
        ProfileUpdateRequest profileUpdateRequest = new ProfileUpdateRequest();
        BecomeAuthorRequest becomeAuthorRequest = new BecomeAuthorRequest();
        ChangeRoleRequest changeRoleRequest = new ChangeRoleRequest();
        changeRoleRequest.setRole(Role.AUTHOR);
        AuthorUpgradeDecisionRequest decisionRequest = new AuthorUpgradeDecisionRequest();
        decisionRequest.setApprove(true);

        when(authService.verifySignupOtp(verifySignupOtpRequest)).thenReturn(AuthResponse.builder().accessToken("x").build());
        when(authService.oauthLogin(oauthLoginRequest)).thenReturn(AuthResponse.builder().accessToken("oauth").build());
        when(authService.updateProfile("admin@example.com", profileUpdateRequest))
                .thenReturn(UserProfileResponse.builder().email("admin@example.com").build());
        when(authService.becomeAuthor("admin@example.com", becomeAuthorRequest))
                .thenReturn(AuthorUpgradeRequestResponse.builder().requestId(1L).build());
        when(authService.getMyAuthorUpgradeRequest("admin@example.com"))
                .thenReturn(AuthorUpgradeRequestResponse.builder().requestId(1L).build());
        lenient().when(authService.searchUsers("a")).thenReturn(List.of());
        when(authService.getUsers("a", Role.AUTHOR, true)).thenReturn(List.of());
        when(authService.getUserCount(Role.AUTHOR, true)).thenReturn(5L);
        when(authService.getUserById(5L)).thenReturn(UserProfileResponse.builder().userId(5L).build());
        when(authService.getPublicUserById(6L)).thenReturn(com.app.authservice.dto.PublicUserProfileResponse.builder().userId(6L).build());
        when(authService.updateRole(5L, changeRoleRequest)).thenReturn(UserProfileResponse.builder().userId(5L).build());
        when(authService.setUserActive(5L, false)).thenReturn(UserProfileResponse.builder().userId(5L).build());
        when(authService.setUserActive(5L, true)).thenReturn(UserProfileResponse.builder().userId(5L).build());
        when(authService.recordAudit(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.app.authservice.dto.AuditLogResponse.builder().auditLogId(1L).build());
        when(authService.getAuditLogs(10)).thenReturn(List.of());
        when(authService.getAuthorUpgradeRequests(AuthorUpgradeStatus.PENDING)).thenReturn(List.of());
        when(authService.getAuthorUpgradeRequestById(1L))
                .thenReturn(AuthorUpgradeRequestResponse.builder().requestId(1L).build());
        when(authService.decideAuthorUpgradeRequest("admin@example.com", 1L, decisionRequest))
                .thenReturn(AuthorUpgradeRequestResponse.builder().requestId(1L).build());

        SimpleApiResponse otpResponse = controller.requestSignupOtp(registerRequest);
        controller.verifySignupOtp(verifySignupOtpRequest);
        controller.oauthLogin(oauthLoginRequest);
        controller.requestForgotPasswordOtp(forgotPasswordOtpRequest);
        controller.resetPasswordWithOtp(resetPasswordWithOtpRequest);
        controller.logout();
        controller.validateToken(null, null);
        controller.updateProfile(authentication, profileUpdateRequest);
        controller.becomeAuthor(authentication, becomeAuthorRequest);
        controller.getMyAuthorRequest(authentication);
        controller.deactivateAccount(authentication);
        controller.getUsers("a", Role.AUTHOR, true);
        controller.getUserCount(Role.AUTHOR, true);
        controller.getUserById(5L);
        controller.getPublicUserById(6L);
        controller.updateRole(5L, changeRoleRequest);
        controller.suspendUser(5L);
        controller.reactivateUser(5L);
        controller.deleteUser(5L);
        controller.recordAudit(authentication, new com.app.authservice.dto.AuditLogRequest());
        controller.getAuditLogs(10);
        controller.getAuthorRequests(AuthorUpgradeStatus.PENDING);
        controller.getAuthorRequestById(1L);
        controller.decideAuthorRequest(authentication, 1L, decisionRequest);

        assertThat(otpResponse.getMessage()).isEqualTo("OTP sent to your email");
        verify(authService).requestSignupOtp(registerRequest);
        verify(authService).verifySignupOtp(verifySignupOtpRequest);
        verify(authService).requestPasswordResetOtp(forgotPasswordOtpRequest);
        verify(authService).resetPasswordWithOtp(resetPasswordWithOtpRequest);
        verify(authService).logout();
        verify(authService).deactivateAccount("admin@example.com");
    }
}


