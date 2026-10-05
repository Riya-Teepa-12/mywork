package com.app.authservice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;

class AuthDtoCoverageTest {

    @Test
    void mutableRequestDtosExposeAccessors() {
        AuditLogRequest auditLogRequest = new AuditLogRequest();
        auditLogRequest.setAction("LOGIN");
        auditLogRequest.setTargetType("USER");
        auditLogRequest.setTargetId(10L);
        auditLogRequest.setDetails("ok");
        assertThat(auditLogRequest.getAction()).isEqualTo("LOGIN");

        OAuthLoginRequest oauth = new OAuthLoginRequest();
        oauth.setProvider(AuthProvider.GITHUB);
        oauth.setEmail("user@example.com");
        oauth.setFullName("User");
        oauth.setUsername("user");
        oauth.setAvatarUrl("http://avatar");
        assertThat(oauth.getProvider()).isEqualTo(AuthProvider.GITHUB);

        BecomeAuthorRequest becomeAuthor = new BecomeAuthorRequest();
        becomeAuthor.setBio("a".repeat(140));
        becomeAuthor.setMotivation("b".repeat(120));
        becomeAuthor.setExpertiseCategories(List.of("java"));
        becomeAuthor.setWritingSampleUrls(List.of("https://example.com/sample"));
        assertThat(becomeAuthor.getExpertiseCategories()).containsExactly("java");

        ResetPasswordWithOtpRequest reset = new ResetPasswordWithOtpRequest();
        reset.setEmail("user@example.com");
        reset.setOtp("123456");
        reset.setNewPassword("Password@1");
        assertThat(reset.getNewPassword()).isEqualTo("Password@1");

        VerifySignupOtpRequest verifySignup = new VerifySignupOtpRequest();
        verifySignup.setEmail("user@example.com");
        verifySignup.setOtp("654321");
        assertThat(verifySignup.getOtp()).isEqualTo("654321");

        AuthorUpgradeDecisionRequest decision = new AuthorUpgradeDecisionRequest();
        decision.setApprove(true);
        decision.setReason("Strong profile");
        assertThat(decision.isApprove()).isTrue();

        ChangeRoleRequest changeRole = new ChangeRoleRequest();
        changeRole.setRole(Role.AUTHOR);
        assertThat(changeRole.getRole()).isEqualTo(Role.AUTHOR);

        ForgotPasswordOtpRequest forgot = new ForgotPasswordOtpRequest();
        forgot.setEmail("forgot@example.com");
        assertThat(forgot.getEmail()).isEqualTo("forgot@example.com");
    }

    @Test
    void builderDtosExposeAccessors() {
        LocalDateTime now = LocalDateTime.now();

        AuditLogResponse audit = AuditLogResponse.builder()
                .auditLogId(1L)
                .actorUserId(2L)
                .actorEmail("admin@example.com")
                .action("ACTION")
                .targetType("USER")
                .targetId(10L)
                .details("details")
                .createdAt(now)
                .build();
        assertThat(audit.getActorEmail()).isEqualTo("admin@example.com");

        AuthorUpgradeRequestResponse authorRequest = AuthorUpgradeRequestResponse.builder()
                .requestId(1L)
                .userId(2L)
                .username("writer")
                .fullName("Writer")
                .email("writer@example.com")
                .bio("bio")
                .motivation("motivation")
                .expertiseCategories(List.of("java"))
                .writingSampleUrls(List.of("https://example.com"))
                .status(AuthorUpgradeStatus.PENDING)
                .decisionReason("n/a")
                .reviewedByUserId(3L)
                .reviewedByName("Admin")
                .reviewedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertThat(authorRequest.getStatus()).isEqualTo(AuthorUpgradeStatus.PENDING);

        SubscriptionSummaryResponse summary = SubscriptionSummaryResponse.builder()
                .subscriptionId(1L)
                .userId(2L)
                .planType(SubscriptionPlanType.NEWSLETTER)
                .status(SubscriptionStatus.ACTIVE)
                .amountPaise(9900L)
                .currency("INR")
                .startsAt(now.minusDays(1))
                .endsAt(now.plusDays(30))
                .providerOrderId("order-1")
                .providerPaymentId("payment-1")
                .createdAt(now)
                .build();
        assertThat(summary.getPlanType()).isEqualTo(SubscriptionPlanType.NEWSLETTER);
    }
}
