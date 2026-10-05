package com.app.authservice.service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthorUpgradeRequestRepository authorUpgradeRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${auth.otp.expire-minutes:10}")
    private long otpExpireMinutes;

    @Value("${auth.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${inkwell.notification.kafka.topic:notification.dispatch.v1}")
    private String notificationKafkaTopic;

    @Value("${spring.application.name:auth-service}")
    private String applicationName;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUsername = normalizeUsername(request.getUsername());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(normalizeFullName(request.getFullName()))
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void requestSignupOtp(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUsername = normalizeUsername(request.getUsername());
        String fullName = normalizeFullName(request.getFullName());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        expirePendingOtps(normalizedEmail, OtpPurpose.SIGNUP);
        EmailOtp otpToken = EmailOtp.builder()
                .email(normalizedEmail)
                .purpose(OtpPurpose.SIGNUP)
                .otpCode(generateOtp())
                .username(normalizedUsername)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpireMinutes))
                .build();
        otpToken = emailOtpRepository.save(otpToken);

        sendPlainEmail(
                normalizedEmail,
                "InkWell signup OTP",
                "Your InkWell signup OTP is: " + otpToken.getOtpCode()
                        + "\n\nThis OTP expires in " + otpExpireMinutes + " minutes.");
    }

    @Override
    @Transactional
    public AuthResponse verifySignupOtp(VerifySignupOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        EmailOtp otpToken = verifyOtp(normalizedEmail, OtpPurpose.SIGNUP, request.getOtp());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (otpToken.getUsername() == null || otpToken.getUsername().isBlank()) {
            throw new IllegalStateException("Signup session is invalid. Please request OTP again.");
        }
        if (otpToken.getPasswordHash() == null || otpToken.getPasswordHash().isBlank()) {
            throw new IllegalStateException("Signup session is invalid. Please request OTP again.");
        }
        if (userRepository.existsByUsername(otpToken.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(otpToken.getUsername())
                .email(normalizedEmail)
                .passwordHash(otpToken.getPasswordHash())
                .fullName(normalizeFullName(otpToken.getFullName()))
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        consumeOtp(otpToken);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated");
        }
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Use OAuth login for this account");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        if (request.getProvider() == AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Invalid OAuth provider");
        }
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUsername = normalizeUsername(request.getUsername());
        String fullName = normalizeFullName(request.getFullName());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .username(normalizedUsername)
                        .fullName(fullName)
                        .passwordHash("")
                        .role(Role.READER)
                        .provider(request.getProvider())
                        .avatarUrl(request.getAvatarUrl())
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (user.getUserId() != null) {
            if (!user.isActive()) {
                throw new IllegalStateException("Account is deactivated");
            }
            if (user.getProvider() != request.getProvider()) {
                throw new IllegalArgumentException("Account is registered with a different provider");
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(request.getAvatarUrl().trim());
            }
            user.setFullName(fullName);
        } else if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void requestPasswordResetOtp(ForgotPasswordOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getProvider() != AuthProvider.LOCAL || !user.isActive()) {
            return;
        }

        expirePendingOtps(normalizedEmail, OtpPurpose.PASSWORD_RESET);
        EmailOtp otpToken = EmailOtp.builder()
                .email(normalizedEmail)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .otpCode(generateOtp())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpireMinutes))
                .build();
        otpToken = emailOtpRepository.save(otpToken);

        sendPlainEmail(
                normalizedEmail,
                "InkWell password reset OTP",
                "Your password reset OTP is: " + otpToken.getOtpCode()
                        + "\n\nThis OTP expires in " + otpExpireMinutes + " minutes.");
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or OTP"));
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password reset is not allowed for OAuth account");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated");
        }

        EmailOtp otpToken = verifyOtp(normalizedEmail, OtpPurpose.PASSWORD_RESET, request.getOtp());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        consumeOtp(otpToken);
    }

    @Override
    public void logout() {
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        boolean valid = jwtUtil.validateToken(token);
        if (!valid) {
            return TokenValidationResponse.builder().valid(false).build();
        }
        return TokenValidationResponse.builder()
                .valid(true)
                .email(jwtUtil.extractEmail(token))
                .role(jwtUtil.extractRole(token))
                .expiresAt(jwtUtil.extractExpiry(token).getTime())
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtUtil.validateToken(request.getToken())) {
            throw new IllegalArgumentException("Invalid token");
        }
        String email = jwtUtil.extractEmail(request.getToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated");
        }
        return buildAuthResponse(user);
    }

    @Override
    public UserProfileResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toProfile(user);
    }

    @Override
    public UserProfileResponse getUserById(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toProfile(user);
    }

    @Override
    public PublicUserProfileResponse getPublicUserById(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isActive()) {
            throw new IllegalArgumentException("User not found");
        }
        if (user.getRole() != Role.AUTHOR && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Author profile not found");
        }
        return toPublicProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim());
        user = userRepository.save(user);
        return toProfile(user);
    }

    @Override
    @Transactional
    public AuthorUpgradeRequestResponse becomeAuthor(String currentEmail, BecomeAuthorRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated");
        }
        if (user.getRole() == Role.AUTHOR) {
            throw new IllegalStateException("You are already an author");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin account already has author capabilities");
        }
        if (user.getRole() != Role.READER) {
            throw new IllegalStateException("Only reader accounts can request author role");
        }
        if (authorUpgradeRequestRepository.existsByUserIdAndStatus(user.getUserId(), AuthorUpgradeStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending author request");
        }

        String bio = normalizeMultiline(request.getBio());
        String motivation = normalizeMultiline(request.getMotivation());
        List<String> categories = normalizeStringList(request.getExpertiseCategories());
        List<String> samples = normalizeStringList(request.getWritingSampleUrls());
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("At least one expertise category is required");
        }
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("At least one writing sample URL is required");
        }

        AuthorUpgradeRequest authorRequest = AuthorUpgradeRequest.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .bio(bio)
                .motivation(motivation)
                .expertiseCategories(joinLines(categories))
                .writingSampleUrls(joinLines(samples))
                .status(AuthorUpgradeStatus.PENDING)
                .build();
        authorRequest = authorUpgradeRequestRepository.save(authorRequest);

        recordAuditInternal(
                "SUBMIT_AUTHOR_REQUEST",
                "AUTHOR_REQUEST",
                authorRequest.getRequestId(),
                "Submitted author request; userId=" + user.getUserId()
                        + "; categories=" + String.join(", ", categories)
                        + "; sampleCount=" + samples.size());

        notifyAdminsAboutAuthorRequest(user, authorRequest);
        return toAuthorUpgradeResponse(authorRequest);
    }

    @Override
    public AuthorUpgradeRequestResponse getMyAuthorUpgradeRequest(String currentEmail) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        AuthorUpgradeRequest authorRequest = authorUpgradeRequestRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getUserId())
                .orElse(null);
        if (authorRequest == null) {
            return null;
        }
        return toAuthorUpgradeResponse(authorRequest);
    }

    @Override
    public List<AuthorUpgradeRequestResponse> getAuthorUpgradeRequests(AuthorUpgradeStatus status) {
        List<AuthorUpgradeRequest> rows = status == null
                ? authorUpgradeRequestRepository.findAllByOrderByCreatedAtDesc()
                : authorUpgradeRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(this::toAuthorUpgradeResponse).toList();
    }

    @Override
    public AuthorUpgradeRequestResponse getAuthorUpgradeRequestById(Long requestId) {
        AuthorUpgradeRequest authorRequest = authorUpgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Author request not found"));
        return toAuthorUpgradeResponse(authorRequest);
    }

    @Override
    @Transactional
    public AuthorUpgradeRequestResponse decideAuthorUpgradeRequest(
            String adminEmail,
            Long requestId,
            AuthorUpgradeDecisionRequest request) {
        User admin = userRepository.findByEmail(normalizeEmail(adminEmail))
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        if (!admin.isActive() || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only active admin can review author requests");
        }

        AuthorUpgradeRequest authorRequest = authorUpgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Author request not found"));
        if (authorRequest.getStatus() != AuthorUpgradeStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be reviewed");
        }

        User requestUser = userRepository.findByUserId(authorRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Requested user not found"));
        boolean approve = request.isApprove();
        String decisionReason = normalizeDecisionReason(request.getReason());
        if (!approve && decisionReason.isBlank()) {
            throw new IllegalArgumentException("Reason is required when rejecting request");
        }

        authorRequest.setReviewedByUserId(admin.getUserId());
        authorRequest.setReviewedByName(admin.getFullName() == null || admin.getFullName().isBlank()
                ? admin.getUsername()
                : admin.getFullName());
        authorRequest.setReviewedAt(LocalDateTime.now());

        if (approve) {
            if (requestUser.getRole() == Role.READER) {
                requestUser.setRole(Role.AUTHOR);
            }
            requestUser.setBio(authorRequest.getBio());
            userRepository.save(requestUser);

            authorRequest.setStatus(AuthorUpgradeStatus.APPROVED);
            authorRequest.setDecisionReason(null);
            authorRequest = authorUpgradeRequestRepository.save(authorRequest);

            recordAuditInternal(
                    "APPROVE_AUTHOR_REQUEST",
                    "AUTHOR_REQUEST",
                    authorRequest.getRequestId(),
                    "Approved by adminUserId=" + admin.getUserId() + "; userId=" + requestUser.getUserId());

            sendNotification(
                    requestUser.getUserId(),
                    admin.getUserId(),
                    "ADMIN_BROADCAST",
                    "Author request approved",
                    "Your author request was approved. You can now access the Author Panel.",
                    authorRequest.getRequestId(),
                    "AUTHOR_REQUEST");
        } else {
            authorRequest.setStatus(AuthorUpgradeStatus.REJECTED);
            authorRequest.setDecisionReason(decisionReason);
            authorRequest = authorUpgradeRequestRepository.save(authorRequest);

            recordAuditInternal(
                    "REJECT_AUTHOR_REQUEST",
                    "AUTHOR_REQUEST",
                    authorRequest.getRequestId(),
                    "Rejected by adminUserId=" + admin.getUserId()
                            + "; userId=" + requestUser.getUserId()
                            + "; reason=" + decisionReason);

            sendNotification(
                    requestUser.getUserId(),
                    admin.getUserId(),
                    "ADMIN_BROADCAST",
                    "Author request rejected",
                    "Your author request was rejected. Reason: " + decisionReason,
                    authorRequest.getRequestId(),
                    "AUTHOR_REQUEST");
        }

        return toAuthorUpgradeResponse(authorRequest);
    }

    @Override
    @Transactional
    public void changePassword(String currentEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password change not allowed for OAuth account");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<UserProfileResponse> searchUsers(String keyword) {
        return getUsers(keyword, null, null);
    }

    @Override
    public List<UserProfileResponse> getUsers(String keyword, Role role, Boolean active) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return userRepository.adminSearchUsers(normalizedKeyword, role, active)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Override
    public long getUserCount(Role role, Boolean active) {
        if (role != null && active != null) {
            return userRepository.countByRoleAndIsActive(role, active);
        }
        if (role != null) {
            return userRepository.countByRole(role);
        }
        if (active != null) {
            return userRepository.countByIsActive(active);
        }
        return userRepository.count();
    }

    @Override
    @Transactional
    public void deactivateAccount(String currentEmail) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateRole(Long userId, ChangeRoleRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(request.getRole());
        user = userRepository.save(user);
        recordAuditInternal(
                "CHANGE_ROLE",
                "USER",
                user.getUserId(),
                "Role changed to " + request.getRole().name());
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse setUserActive(Long userId, boolean active) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(active);
        user = userRepository.save(user);
        recordAuditInternal(
                active ? "REACTIVATE_USER" : "SUSPEND_USER",
                "USER",
                user.getUserId(),
                active ? "User reactivated" : "User suspended");
        return toProfile(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.deleteByUserId(userId);
        recordAuditInternal("DELETE_USER", "USER", userId, "Deleted user " + user.getEmail());
    }

    @Override
    @Transactional
    public AuditLogResponse recordAudit(String actorEmail, AuditLogRequest request) {
        return toAuditResponse(saveAudit(actorEmail, request));
    }

    @Override
    public List<AuditLogResponse> getAuditLogs(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 500));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, normalizedLimit))
                .stream()
                .map(this::toAuditResponse)
                .toList();
    }

    private void notifyAdminsAboutAuthorRequest(User requester, AuthorUpgradeRequest authorRequest) {
        List<User> admins = userRepository.findAllByRole(Role.ADMIN).stream()
                .filter(User::isActive)
                .toList();
        String requesterName = requester.getFullName() == null || requester.getFullName().isBlank()
                ? requester.getUsername()
                : requester.getFullName();
        String title = "New author request received";
        String message = requesterName + " requested author access. Open Admin > Author Requests to review.";
        for (User admin : admins) {
            if (admin.getUserId() == null) {
                continue;
            }
            sendNotification(
                    admin.getUserId(),
                    requester.getUserId(),
                    "ADMIN_BROADCAST",
                    title,
                    message,
                    authorRequest.getRequestId(),
                    "AUTHOR_REQUEST");
        }
    }

    private void sendNotification(
            Long recipientId,
            Long actorId,
            String type,
            String title,
            String message,
            Long relatedId,
            String relatedType) {
        if (recipientId == null || actorId == null) {
            return;
        }
        NotificationDispatchEvent inAppEvent = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("IN_APP")
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(inAppEvent);

        NotificationDispatchEvent emailEvent = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("EMAIL")
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(emailEvent);
    }

    private void publishNotificationEvent(NotificationDispatchEvent event) {
        if (notificationKafkaTemplateProvider == null) {
            return;
        }
        KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate =
                notificationKafkaTemplateProvider.getIfAvailable();
        if (notificationKafkaTemplate == null) {
            return;
        }
        String key = event.getRecipientId() == null ? UUID.randomUUID().toString() : String.valueOf(event.getRecipientId());
        notificationKafkaTemplate.send(notificationKafkaTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed for auth notification event {}", event.getEventId(), ex);
                    }
                });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? "" : fullName.trim();
    }

    private String normalizeMultiline(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDecisionReason(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 1000) {
            return normalized.substring(0, 1000);
        }
        return normalized;
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String joinLines(List<String> values) {
        return String.join("\n", values);
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private void sendPlainEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send OTP email. Please try again.");
        }
    }

    private void expirePendingOtps(String email, OtpPurpose purpose) {
        List<EmailOtp> tokens = emailOtpRepository.findByEmailAndPurposeAndConsumedFalse(email, purpose);
        if (tokens.isEmpty()) {
            return;
        }
        tokens.forEach(token -> token.setConsumed(true));
        emailOtpRepository.saveAll(tokens);
    }

    private EmailOtp verifyOtp(String email, OtpPurpose purpose, String otpInput) {
        EmailOtp token = emailOtpRepository
                .findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("OTP is invalid or expired"));

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setConsumed(true);
            emailOtpRepository.save(token);
            throw new IllegalArgumentException("OTP has expired");
        }

        if (token.getAttemptCount() != null && token.getAttemptCount() >= otpMaxAttempts) {
            token.setConsumed(true);
            emailOtpRepository.save(token);
            throw new IllegalArgumentException("OTP attempts exceeded. Request a new OTP.");
        }

        String normalizedInput = otpInput == null ? "" : otpInput.trim();
        if (!normalizedInput.equals(token.getOtpCode())) {
            int attempts = token.getAttemptCount() == null ? 0 : token.getAttemptCount();
            token.setAttemptCount(attempts + 1);
            if (token.getAttemptCount() >= otpMaxAttempts) {
                token.setConsumed(true);
            }
            emailOtpRepository.save(token);
            throw new IllegalArgumentException("Invalid OTP");
        }
        return token;
    }

    private void consumeOtp(EmailOtp token) {
        token.setConsumed(true);
        emailOtpRepository.save(token);
    }

    private void recordAuditInternal(String action, String targetType, Long targetId, String details) {
        String actorEmail = currentActorEmail();
        if (actorEmail == null || actorEmail.isBlank()) {
            return;
        }
        AuditLogRequest request = new AuditLogRequest();
        request.setAction(action);
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        request.setDetails(details);
        saveAudit(actorEmail, request);
    }

    private AuditLog saveAudit(String actorEmail, AuditLogRequest request) {
        User actor = userRepository.findByEmail(normalizeEmail(actorEmail)).orElse(null);
        AuditLog audit = AuditLog.builder()
                .actorUserId(actor == null ? null : actor.getUserId())
                .actorEmail(actor == null ? normalizeEmail(actorEmail) : actor.getEmail())
                .action(request.getAction().trim())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .details(request.getDetails())
                .build();
        return auditLogRepository.save(audit);
    }

    private String currentActorEmail() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof String value) {
                return value;
            }
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresAt(jwtUtil.extractExpiry(token).getTime())
                .user(toProfile(user))
                .build();
    }

    private UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .active(user.isActive())
                .build();
    }

    private AuthorUpgradeRequestResponse toAuthorUpgradeResponse(AuthorUpgradeRequest row) {
        return AuthorUpgradeRequestResponse.builder()
                .requestId(row.getRequestId())
                .userId(row.getUserId())
                .username(row.getUsername())
                .fullName(row.getFullName())
                .email(row.getEmail())
                .bio(row.getBio())
                .motivation(row.getMotivation())
                .expertiseCategories(splitLines(row.getExpertiseCategories()))
                .writingSampleUrls(splitLines(row.getWritingSampleUrls()))
                .status(row.getStatus())
                .decisionReason(row.getDecisionReason())
                .reviewedByUserId(row.getReviewedByUserId())
                .reviewedByName(row.getReviewedByName())
                .reviewedAt(row.getReviewedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private AuditLogResponse toAuditResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .auditLogId(auditLog.getAuditLogId())
                .actorUserId(auditLog.getActorUserId())
                .actorEmail(auditLog.getActorEmail())
                .action(auditLog.getAction())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private PublicUserProfileResponse toPublicProfile(User user) {
        return PublicUserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

}
