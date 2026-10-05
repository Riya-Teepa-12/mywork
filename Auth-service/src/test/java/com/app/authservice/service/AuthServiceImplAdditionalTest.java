package com.app.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.authservice.dto.ChangePasswordRequest;
import com.app.authservice.dto.OAuthLoginRequest;
import com.app.authservice.dto.RefreshTokenRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordWithOtpRequest;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.AuthorUpgradeRequest;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.entity.EmailOtp;
import com.app.authservice.entity.OtpPurpose;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.User;
import com.app.authservice.repository.AuditLogRepository;
import com.app.authservice.repository.AuthorUpgradeRequestRepository;
import com.app.authservice.repository.EmailOtpRepository;
import com.app.authservice.repository.UserRepository;
import com.app.authservice.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplAdditionalTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailOtpRepository emailOtpRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthorUpgradeRequestRepository authorUpgradeRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "mailFrom", "auth@inkwell.com");
        ReflectionTestUtils.setField(authService, "otpExpireMinutes", 10L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);
    }

    @Test
    void registerRejectsExistingEmailOrUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setUsername("existing");
        request.setPassword("Password@1");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void oauthLoginUpdatesExistingUserDetails() {
        OAuthLoginRequest request = new OAuthLoginRequest();
        request.setProvider(AuthProvider.GITHUB);
        request.setEmail("oauth@example.com");
        request.setUsername("oauth");
        request.setFullName("OAuth User");
        request.setAvatarUrl(" http://avatar ");

        User existing = User.builder()
                .userId(10L)
                .email("oauth@example.com")
                .username("oauth")
                .fullName("Old Name")
                .avatarUrl("old")
                .provider(AuthProvider.GITHUB)
                .isActive(true)
                .role(Role.READER)
                .build();

        when(userRepository.findByEmail("oauth@example.com")).thenReturn(java.util.Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("token");
        when(jwtUtil.extractExpiry("token")).thenReturn(new Date(1234L));

        assertThat(authService.oauthLogin(request).getAccessToken()).isEqualTo("token");
        assertThat(existing.getAvatarUrl()).isEqualTo("http://avatar");
        assertThat(existing.getFullName()).isEqualTo("OAuth User");
    }

    @Test
    void oauthLoginRejectsInactiveUser() {
        OAuthLoginRequest request = new OAuthLoginRequest();
        request.setProvider(AuthProvider.GITHUB);
        request.setEmail("inactive@example.com");
        request.setUsername("inactive");
        request.setFullName("Inactive");

        User inactive = User.builder()
                .userId(11L)
                .email("inactive@example.com")
                .provider(AuthProvider.GITHUB)
                .isActive(false)
                .build();

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(java.util.Optional.of(inactive));

        assertThatThrownBy(() -> authService.oauthLogin(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void verifySignupOtpRejectsInvalidSessionData() {
        EmailOtp missingUsername = EmailOtp.builder()
                .email("nouser@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("123456")
                .passwordHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .consumed(false)
                .build();
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "nouser@example.com", OtpPurpose.SIGNUP)).thenReturn(java.util.Optional.of(missingUsername));
        when(userRepository.existsByEmail("nouser@example.com")).thenReturn(false);

        VerifySignupOtpRequest request = new VerifySignupOtpRequest();
        request.setEmail("nouser@example.com");
        request.setOtp("123456");
        assertThatThrownBy(() -> authService.verifySignupOtp(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Signup session is invalid");

        EmailOtp missingPassword = EmailOtp.builder()
                .email("nopass@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("654321")
                .username("user")
                .passwordHash(" ")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .consumed(false)
                .build();
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "nopass@example.com", OtpPurpose.SIGNUP)).thenReturn(java.util.Optional.of(missingPassword));
        when(userRepository.existsByEmail("nopass@example.com")).thenReturn(false);

        VerifySignupOtpRequest request2 = new VerifySignupOtpRequest();
        request2.setEmail("nopass@example.com");
        request2.setOtp("654321");
        assertThatThrownBy(() -> authService.verifySignupOtp(request2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Signup session is invalid");
    }

    @Test
    void resetPasswordWithOtpRejectsOauthOrInactiveAccounts() {
        ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
        request.setEmail("oauth@example.com");
        request.setOtp("111111");
        request.setNewPassword("Password@1");

        User oauthUser = User.builder()
                .email("oauth@example.com")
                .provider(AuthProvider.GOOGLE)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(java.util.Optional.of(oauthUser));

        assertThatThrownBy(() -> authService.resetPasswordWithOtp(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth");

        User inactiveLocal = User.builder()
                .email("inactive@example.com")
                .provider(AuthProvider.LOCAL)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(java.util.Optional.of(inactiveLocal));
        request.setEmail("inactive@example.com");

        assertThatThrownBy(() -> authService.resetPasswordWithOtp(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void changePasswordRejectsOauthAndMismatchedPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("new");

        User oauthUser = User.builder()
                .email("oauth@example.com")
                .provider(AuthProvider.GOOGLE)
                .isActive(true)
                .passwordHash("hash")
                .build();
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(java.util.Optional.of(oauthUser));

        assertThatThrownBy(() -> authService.changePassword("oauth@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth");

        User localUser = User.builder()
                .email("local@example.com")
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .passwordHash("hash")
                .build();
        when(userRepository.findByEmail("local@example.com")).thenReturn(java.util.Optional.of(localUser));
        when(passwordEncoder.matches("current", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("local@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password");
        verify(userRepository, never()).save(localUser);
    }

    @Test
    void authorRequestLookupBranchesAreHandled() {
        User user = User.builder()
                .userId(5L)
                .email("reader@example.com")
                .build();
        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(user));
        when(authorUpgradeRequestRepository.findTopByUserIdOrderByCreatedAtDesc(5L))
                .thenReturn(java.util.Optional.empty());

        assertThat(authService.getMyAuthorUpgradeRequest("reader@example.com")).isNull();

        AuthorUpgradeRequest pending = AuthorUpgradeRequest.builder()
                .requestId(1L)
                .userId(5L)
                .status(AuthorUpgradeStatus.PENDING)
                .build();
        when(authorUpgradeRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pending));
        when(authorUpgradeRequestRepository.findByStatusOrderByCreatedAtDesc(AuthorUpgradeStatus.PENDING))
                .thenReturn(List.of(pending));

        assertThat(authService.getAuthorUpgradeRequests(null)).hasSize(1);
        assertThat(authService.getAuthorUpgradeRequests(AuthorUpgradeStatus.PENDING)).hasSize(1);

        when(authorUpgradeRequestRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> authService.getAuthorUpgradeRequestById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Author request not found");
    }

    @Test
    void searchUsersDelegatesToRepository() {
        User user = User.builder()
                .userId(1L)
                .username("user")
                .email("user@example.com")
                .role(Role.READER)
                .isActive(true)
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.adminSearchUsers("john", null, null)).thenReturn(List.of(user));

        assertThat(authService.searchUsers("john")).hasSize(1);
        assertThat(authService.getUsers("john", null, null).get(0).getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void refreshTokenRejectsUnknownUser() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken("token");
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.extractEmail("token")).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}

