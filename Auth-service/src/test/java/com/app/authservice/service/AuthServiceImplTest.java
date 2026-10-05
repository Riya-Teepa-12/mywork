package com.app.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.authservice.dto.AuditLogRequest;
import com.app.authservice.dto.AuthorUpgradeDecisionRequest;
import com.app.authservice.dto.BecomeAuthorRequest;
import com.app.authservice.dto.ChangeRoleRequest;
import com.app.authservice.dto.ForgotPasswordOtpRequest;
import com.app.authservice.dto.ChangePasswordRequest;
import com.app.authservice.dto.LoginRequest;
import com.app.authservice.dto.ProfileUpdateRequest;
import com.app.authservice.dto.RefreshTokenRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordWithOtpRequest;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.dto.TokenValidationResponse;
import com.app.authservice.dto.UserProfileResponse;
import com.app.authservice.entity.AuditLog;
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
import com.app.authservice.messaging.NotificationDispatchEvent;
import com.app.authservice.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

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

    @Mock
    @SuppressWarnings("unused")
    private ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(authService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        ReflectionTestUtils.setField(authService, "mailFrom", "auth@inkwell.com");
        ReflectionTestUtils.setField(authService, "otpExpireMinutes", 10L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);
        ReflectionTestUtils.setField(authService, "notificationKafkaTopic", "notification.dispatch.v1");
        ReflectionTestUtils.setField(authService, "applicationName", "auth-service");
    }

    @Test
    void registerLoginAndTokenValidationWork() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("jane");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Jane Doe");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("jane")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("token");
        when(jwtUtil.extractExpiry(any(String.class))).thenReturn(new Date());

        UserProfileResponse user = authService.register(registerRequest).getUser();
        assertThat(user.getEmail()).isEqualTo("jane@example.com");

        User existing = User.builder()
                .userId(2L)
                .username("jane")
                .email("jane@example.com")
                .passwordHash("encoded").fullName("Jane Doe")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(java.util.Optional.of(existing));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        assertThat(authService.login(loginRequest()).getAccessToken()).isEqualTo("token");

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("jane@example.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("READER");
        when(jwtUtil.extractExpiry("valid-token")).thenReturn(new Date(123456789L));

        TokenValidationResponse validation = authService.validateToken("valid-token");
        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void updateProfileAndChangePasswordUseStoredUser() {
        User user = User.builder()
                .userId(1L)
                .username("jane")
                .email("jane@example.com")
                .passwordHash("encoded")
                .fullName("Jane Doe")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("current", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        org.assertj.core.api.Assertions.assertThat(authService.updateProfile("jane@example.com", profileRequest()).getEmail()).isEqualTo("jane@example.com");
        authService.changePassword("jane@example.com", changePasswordRequest());
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void refreshTokenAndGetPublicProfileUseJwtAndUserLookup() {
        User user = User.builder()
                .userId(3L)
                .username("author")
                .email("author@example.com")
                .passwordHash("encoded")
                .fullName("Author")
                .role(Role.AUTHOR)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractEmail("refresh-token")).thenReturn("author@example.com");
        when(userRepository.findByEmail("author@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("token");
        when(jwtUtil.extractExpiry(any(String.class))).thenReturn(new Date());
        when(userRepository.findByUserId(3L)).thenReturn(java.util.Optional.of(user));

        assertThat(authService.refreshToken(refreshTokenRequest()).getAccessToken()).isEqualTo("token");
        assertThat(authService.getPublicUserById(3L).getUsername()).isEqualTo("author");
    }

    @Test
    void requestSignupOtpAndVerifySignupOtpCreateUser() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("jane");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Jane Doe");

        EmailOtp otpToken = EmailOtp.builder()
                .otpId(1L)
                .email("jane@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("123456")
                .username("jane")
                .fullName("Jane Doe")
                .passwordHash("encoded")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .consumed(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("jane")).thenReturn(false);
        when(emailOtpRepository.findByEmailAndPurposeAndConsumedFalse("jane@example.com", OtpPurpose.SIGNUP))
                .thenReturn(List.of());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(emailOtpRepository.save(any(EmailOtp.class))).thenReturn(otpToken);
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "jane@example.com", OtpPurpose.SIGNUP)).thenReturn(java.util.Optional.of(otpToken));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return user;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("token");
        when(jwtUtil.extractExpiry(any(String.class))).thenReturn(new Date());

        authService.requestSignupOtp(registerRequest);

        VerifySignupOtpRequest verifyRequest = new VerifySignupOtpRequest();
        verifyRequest.setEmail("jane@example.com");
        verifyRequest.setOtp("123456");
        UserProfileResponse user = authService.verifySignupOtp(verifyRequest).getUser();

        assertThat(user.getUserId()).isEqualTo(100L);
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(emailOtpRepository, times(2)).save(any(EmailOtp.class));
    }

    @Test
    void requestPasswordResetOtpSendsMailOnlyForActiveLocalUser() {
        ForgotPasswordOtpRequest request = new ForgotPasswordOtpRequest();
        request.setEmail("reader@example.com");
        User localUser = User.builder()
                .userId(5L)
                .email("reader@example.com")
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        EmailOtp otpToken = EmailOtp.builder()
                .otpCode("123456")
                .email("reader@example.com")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .consumed(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(localUser));
        when(emailOtpRepository.findByEmailAndPurposeAndConsumedFalse("reader@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(List.of());
        when(emailOtpRepository.save(any(EmailOtp.class))).thenReturn(otpToken);

        authService.requestPasswordResetOtp(request);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPasswordWithOtpUpdatesPasswordAndConsumesToken() {
        ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
        request.setEmail("reader@example.com");
        request.setOtp("123456");
        request.setNewPassword("Password@1");
        User user = User.builder()
                .userId(5L)
                .email("reader@example.com")
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .passwordHash("old-hash")
                .build();
        EmailOtp otpToken = EmailOtp.builder()
                .otpId(11L)
                .email("reader@example.com")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .attemptCount(0)
                .consumed(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(user));
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "reader@example.com", OtpPurpose.PASSWORD_RESET)).thenReturn(java.util.Optional.of(otpToken));
        when(passwordEncoder.encode("Password@1")).thenReturn("new-hash");

        authService.resetPasswordWithOtp(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(emailOtpRepository).save(any(EmailOtp.class));
    }

    @Test
    void oauthLoginRejectsLocalProvider() {
        com.app.authservice.dto.OAuthLoginRequest request = new com.app.authservice.dto.OAuthLoginRequest();
        request.setProvider(AuthProvider.LOCAL);
        request.setEmail("oauth@example.com");
        request.setUsername("oauth-user");
        request.setFullName("OAuth User");

        assertThatThrownBy(() -> authService.oauthLogin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OAuth provider");
    }

    @Test
    void becomeAuthorCreatesPendingRequestForReader() {
        User user = User.builder()
                .userId(8L)
                .username("reader")
                .fullName("Reader")
                .email("reader@example.com")
                .role(Role.READER)
                .isActive(true)
                .build();
        BecomeAuthorRequest request = new BecomeAuthorRequest();
        request.setBio("a".repeat(140));
        request.setMotivation("b".repeat(100));
        request.setExpertiseCategories(List.of("Java", "Spring"));
        request.setWritingSampleUrls(List.of("https://example.com/one"));
        AuthorUpgradeRequest saved = AuthorUpgradeRequest.builder()
                .requestId(51L)
                .userId(8L)
                .username("reader")
                .fullName("Reader")
                .email("reader@example.com")
                .bio("bio")
                .motivation("motivation")
                .expertiseCategories("Java")
                .writingSampleUrls("https://example.com/one")
                .status(AuthorUpgradeStatus.PENDING)
                .build();

        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(user));
        when(authorUpgradeRequestRepository.existsByUserIdAndStatus(8L, AuthorUpgradeStatus.PENDING)).thenReturn(false);
        when(authorUpgradeRequestRepository.save(any(AuthorUpgradeRequest.class))).thenReturn(saved);
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        assertThat(authService.becomeAuthor("reader@example.com", request).getRequestId()).isEqualTo(51L);
    }

    @Test
    void becomeAuthorNotifiesActiveAdminsThroughKafka() {
        ReflectionTestUtils.setField(authService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        when(notificationKafkaTemplateProvider.getIfAvailable()).thenReturn(notificationKafkaTemplate);
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
                .when(notificationKafkaTemplate)
                .send(org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.any(NotificationDispatchEvent.class));

        User reader = User.builder()
                .userId(8L)
                .username("reader")
                .fullName("Reader")
                .email("reader@example.com")
                .role(Role.READER)
                .isActive(true)
                .build();
        User activeAdmin = User.builder().userId(501L).role(Role.ADMIN).isActive(true).fullName("Admin").build();
        User inactiveAdmin = User.builder().userId(502L).role(Role.ADMIN).isActive(false).fullName("Sleep").build();
        User noIdAdmin = User.builder().userId(null).role(Role.ADMIN).isActive(true).fullName("NoId").build();

        BecomeAuthorRequest request = new BecomeAuthorRequest();
        request.setBio("bio bio bio");
        request.setMotivation("motivation motivation");
        request.setExpertiseCategories(List.of("Java"));
        request.setWritingSampleUrls(List.of("https://example.com/sample"));
        AuthorUpgradeRequest saved = AuthorUpgradeRequest.builder()
                .requestId(61L)
                .userId(8L)
                .username("reader")
                .fullName("Reader")
                .email("reader@example.com")
                .bio("bio")
                .motivation("motivation")
                .expertiseCategories("Java")
                .writingSampleUrls("https://example.com/sample")
                .status(AuthorUpgradeStatus.PENDING)
                .build();

        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(reader));
        when(authorUpgradeRequestRepository.existsByUserIdAndStatus(8L, AuthorUpgradeStatus.PENDING)).thenReturn(false);
        when(authorUpgradeRequestRepository.save(any(AuthorUpgradeRequest.class))).thenReturn(saved);
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(activeAdmin, inactiveAdmin, noIdAdmin));

        authService.becomeAuthor("reader@example.com", request);

        verify(notificationKafkaTemplate, times(2))
                .send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    @Test
    void decideAuthorUpgradeRequestRequiresReasonOnRejection() {
        User admin = User.builder()
                .userId(99L)
                .email("admin@example.com")
                .fullName("Admin")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        User requester = User.builder()
                .userId(10L)
                .email("reader@example.com")
                .role(Role.READER)
                .isActive(true)
                .build();
        AuthorUpgradeRequest authorRequest = AuthorUpgradeRequest.builder()
                .requestId(1L)
                .userId(10L)
                .status(AuthorUpgradeStatus.PENDING)
                .build();
        AuthorUpgradeDecisionRequest decision = new AuthorUpgradeDecisionRequest();
        decision.setApprove(false);
        decision.setReason(" ");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(java.util.Optional.of(admin));
        when(authorUpgradeRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(authorRequest));
        when(userRepository.findByUserId(10L)).thenReturn(java.util.Optional.of(requester));

        assertThatThrownBy(() -> authService.decideAuthorUpgradeRequest("admin@example.com", 1L, decision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason is required");
        verify(authorUpgradeRequestRepository, never()).save(any(AuthorUpgradeRequest.class));
    }

    @Test
    void getUserCountSupportsAllFilterCombinations() {
        when(userRepository.countByRoleAndIsActive(Role.AUTHOR, true)).thenReturn(2L);
        when(userRepository.countByRole(Role.AUTHOR)).thenReturn(3L);
        when(userRepository.countByIsActive(true)).thenReturn(5L);
        when(userRepository.count()).thenReturn(9L);

        assertThat(authService.getUserCount(Role.AUTHOR, true)).isEqualTo(2L);
        assertThat(authService.getUserCount(Role.AUTHOR, null)).isEqualTo(3L);
        assertThat(authService.getUserCount(null, true)).isEqualTo(5L);
        assertThat(authService.getUserCount(null, null)).isEqualTo(9L);
    }

    @Test
    void loginAndRefreshTokenRejectInvalidStates() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("missing@example.com");
        loginRequest.setPassword("x");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");

        User inactiveUser = User.builder()
                .userId(10L)
                .email("inactive@example.com")
                .passwordHash("hash")
                .provider(AuthProvider.LOCAL)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(java.util.Optional.of(inactiveUser));
        loginRequest.setEmail("inactive@example.com");
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivated");

        RefreshTokenRequest refresh = new RefreshTokenRequest();
        refresh.setToken("invalid");
        when(jwtUtil.validateToken("invalid")).thenReturn(false);
        assertThatThrownBy(() -> authService.refreshToken(refresh))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void getPublicUserByIdRejectsInactiveOrNonAuthor() {
        User inactive = User.builder()
                .userId(20L)
                .username("inactive")
                .email("inactive@example.com")
                .role(Role.AUTHOR)
                .isActive(false)
                .build();
        when(userRepository.findByUserId(20L)).thenReturn(java.util.Optional.of(inactive));

        assertThatThrownBy(() -> authService.getPublicUserById(20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        User reader = User.builder()
                .userId(21L)
                .username("reader")
                .email("reader@example.com")
                .role(Role.READER)
                .isActive(true)
                .build();
        when(userRepository.findByUserId(21L)).thenReturn(java.util.Optional.of(reader));

        assertThatThrownBy(() -> authService.getPublicUserById(21L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Author profile not found");
    }

    @Test
    void requestPasswordResetOtpSkipsOAuthOrInactiveUser() {
        ForgotPasswordOtpRequest request = new ForgotPasswordOtpRequest();
        request.setEmail("oauth@example.com");
        User oauthUser = User.builder()
                .email("oauth@example.com")
                .provider(AuthProvider.GOOGLE)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(java.util.Optional.of(oauthUser));

        authService.requestPasswordResetOtp(request);

        request.setEmail("inactive@example.com");
        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .provider(AuthProvider.LOCAL)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(java.util.Optional.of(inactiveUser));
        authService.requestPasswordResetOtp(request);

        verify(emailOtpRepository, never()).save(any(EmailOtp.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void verifySignupOtpHandlesExpiredAndInvalidOtpCases() {
        EmailOtp expired = EmailOtp.builder()
                .email("expired@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("111111")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .consumed(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "expired@example.com", OtpPurpose.SIGNUP)).thenReturn(java.util.Optional.of(expired));

        VerifySignupOtpRequest expiredReq = new VerifySignupOtpRequest();
        expiredReq.setEmail("expired@example.com");
        expiredReq.setOtp("111111");
        assertThatThrownBy(() -> authService.verifySignupOtp(expiredReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        EmailOtp invalid = EmailOtp.builder()
                .email("invalid@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("222222")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .consumed(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "invalid@example.com", OtpPurpose.SIGNUP)).thenReturn(java.util.Optional.of(invalid));

        VerifySignupOtpRequest invalidReq = new VerifySignupOtpRequest();
        invalidReq.setEmail("invalid@example.com");
        invalidReq.setOtp("999999");
        assertThatThrownBy(() -> authService.verifySignupOtp(invalidReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void oauthLoginRejectsProviderMismatchAndUsernameConflict() {
        com.app.authservice.dto.OAuthLoginRequest mismatch = new com.app.authservice.dto.OAuthLoginRequest();
        mismatch.setProvider(AuthProvider.GITHUB);
        mismatch.setEmail("user@example.com");
        mismatch.setUsername("user");
        mismatch.setFullName("User");
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(
                User.builder()
                        .userId(30L)
                        .email("user@example.com")
                        .provider(AuthProvider.GOOGLE)
                        .isActive(true)
                        .build()));

        assertThatThrownBy(() -> authService.oauthLogin(mismatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different provider");

        com.app.authservice.dto.OAuthLoginRequest conflict = new com.app.authservice.dto.OAuthLoginRequest();
        conflict.setProvider(AuthProvider.GITHUB);
        conflict.setEmail("new@example.com");
        conflict.setUsername("taken");
        conflict.setFullName("Taken Name");
        when(userRepository.findByEmail("new@example.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.oauthLogin(conflict))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void decideAuthorUpgradeRequestApprovePathPromotesReader() {
        User admin = User.builder()
                .userId(99L)
                .email("admin@example.com")
                .fullName("Admin")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        User requester = User.builder()
                .userId(10L)
                .email("reader@example.com")
                .username("reader")
                .role(Role.READER)
                .isActive(true)
                .build();
        AuthorUpgradeRequest authorRequest = AuthorUpgradeRequest.builder()
                .requestId(1L)
                .userId(10L)
                .bio("bio")
                .status(AuthorUpgradeStatus.PENDING)
                .build();
        AuthorUpgradeDecisionRequest decision = new AuthorUpgradeDecisionRequest();
        decision.setApprove(true);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(java.util.Optional.of(admin));
        when(authorUpgradeRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(authorRequest));
        when(userRepository.findByUserId(10L)).thenReturn(java.util.Optional.of(requester));
        when(authorUpgradeRequestRepository.save(any(AuthorUpgradeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(authService.decideAuthorUpgradeRequest("admin@example.com", 1L, decision).getStatus())
                .isEqualTo(AuthorUpgradeStatus.APPROVED);
        assertThat(requester.getRole()).isEqualTo(Role.AUTHOR);
    }

    @Test
    void adminAndAuditOperationsUseRepositories() {
        User admin = User.builder()
                .userId(100L)
                .email("admin@example.com")
                .username("admin")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        User target = User.builder()
                .userId(101L)
                .email("reader@example.com")
                .username("reader")
                .role(Role.READER)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(java.util.Optional.of(admin));
        when(userRepository.findByUserId(101L)).thenReturn(java.util.Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog auditLog = invocation.getArgument(0);
            auditLog.setAuditLogId(1L);
            auditLog.setCreatedAt(LocalDateTime.now());
            return auditLog;
        });
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(AuditLog.builder().auditLogId(1L).action("A").createdAt(LocalDateTime.now()).build()));

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.com", "n/a"));

        ChangeRoleRequest changeRole = new ChangeRoleRequest();
        changeRole.setRole(Role.AUTHOR);
        assertThat(authService.updateRole(101L, changeRole).getRole()).isEqualTo(Role.AUTHOR);
        assertThat(authService.setUserActive(101L, false).isActive()).isFalse();

        ProfileUpdateRequest profile = new ProfileUpdateRequest();
        profile.setBio("new bio");
        profile.setAvatarUrl(" http://avatar ");
        when(userRepository.findByEmail("reader@example.com")).thenReturn(java.util.Optional.of(target));
        assertThat(authService.updateProfile("reader@example.com", profile).getAvatarUrl()).isEqualTo("http://avatar");

        AuditLogRequest request = new AuditLogRequest();
        request.setAction("ACTION");
        request.setTargetType("USER");
        request.setTargetId(101L);
        request.setDetails("details");
        assertThat(authService.recordAudit("admin@example.com", request).getAuditLogId()).isEqualTo(1L);
        assertThat(authService.getAuditLogs(5)).hasSize(1);

        authService.deleteUser(101L);
        verify(userRepository).deleteByUserId(101L);
    }

    @Test
    void becomeAuthorRejectsUnsupportedRolesAndInvalidInput() {
        User author = User.builder()
                .userId(8L)
                .email("author@example.com")
                .role(Role.AUTHOR)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("author@example.com")).thenReturn(java.util.Optional.of(author));

        BecomeAuthorRequest request = new BecomeAuthorRequest();
        request.setBio("bio");
        request.setMotivation("motivation");
        request.setExpertiseCategories(List.of(" "));
        request.setWritingSampleUrls(List.of(" "));

        assertThatThrownBy(() -> authService.becomeAuthor("author@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already an author");

        User reader = User.builder()
                .userId(9L)
                .email("reader2@example.com")
                .role(Role.READER)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("reader2@example.com")).thenReturn(java.util.Optional.of(reader));
        when(authorUpgradeRequestRepository.existsByUserIdAndStatus(9L, AuthorUpgradeStatus.PENDING)).thenReturn(false);

        assertThatThrownBy(() -> authService.becomeAuthor("reader2@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expertise category");
    }

    @Test
    void publishNotificationEventHandlesNullRecipientAndMissingProvider() {
        ReflectionTestUtils.setField(authService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        when(notificationKafkaTemplateProvider.getIfAvailable()).thenReturn(notificationKafkaTemplate);
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
                .when(notificationKafkaTemplate)
                .send(org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.any(NotificationDispatchEvent.class));

        NotificationDispatchEvent event = NotificationDispatchEvent.builder().recipientId(null).build();
        ReflectionTestUtils.invokeMethod(authService, "publishNotificationEvent", event);
        verify(notificationKafkaTemplate).send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));

        ReflectionTestUtils.setField(authService, "notificationKafkaTemplateProvider", null);
        ReflectionTestUtils.invokeMethod(authService, "publishNotificationEvent", event);
        verify(notificationKafkaTemplate, times(1))
                .send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        return request;
    }

    private ChangePasswordRequest changePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("newpassword");
        return request;
    }

    private com.app.authservice.dto.ProfileUpdateRequest profileRequest() {
        com.app.authservice.dto.ProfileUpdateRequest request = new com.app.authservice.dto.ProfileUpdateRequest();
        request.setBio("Bio");
        request.setAvatarUrl("http://avatar");
        return request;
    }

    private RefreshTokenRequest refreshTokenRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken("refresh-token");
        return request;
    }
}
