package com.app.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.app.authservice.entity.AuditLog;
import com.app.authservice.entity.EmailOtp;
import com.app.authservice.entity.OtpPurpose;
import com.app.authservice.AuthServiceApplication;

@SpringBootTest(classes = AuthServiceApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan(basePackageClasses = EmailOtp.class)
@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection"})
class EmailOtpAuditLogRepositoryTest {

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void otpAndAuditLogQueriesWork() {
        EmailOtp otp = emailOtpRepository.saveAndFlush(EmailOtp.builder()
                .email("user@example.com")
                .purpose(OtpPurpose.SIGNUP)
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .consumed(false)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build());

        AuditLog auditLog = auditLogRepository.saveAndFlush(AuditLog.builder()
                .actorUserId(1L)
                .actorEmail("admin@example.com")
                .action("DELETE_USER")
                .targetType("USER")
                .targetId(2L)
                .details("Deleted user")
                .createdAt(LocalDateTime.now())
                .build());

        assertThat(emailOtpRepository.findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc("user@example.com", OtpPurpose.SIGNUP))
                .isPresent()
                .get()
                .extracting(EmailOtp::getEmail, EmailOtp::getOtpCode, EmailOtp::getPurpose)
                .containsExactly("user@example.com", "123456", OtpPurpose.SIGNUP);
        assertThat(emailOtpRepository.findByEmailAndPurposeAndConsumedFalse("user@example.com", OtpPurpose.SIGNUP))
                .hasSize(1)
                .first()
                .extracting(EmailOtp::getEmail, EmailOtp::getOtpCode, EmailOtp::getPurpose)
                .containsExactly("user@example.com", "123456", OtpPurpose.SIGNUP);
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)))
                .hasSize(1)
                .first()
                .extracting(AuditLog::getActorUserId, AuditLog::getActorEmail, AuditLog::getAction, AuditLog::getTargetType, AuditLog::getTargetId, AuditLog::getDetails)
                .containsExactly(1L, "admin@example.com", "DELETE_USER", "USER", 2L, "Deleted user");
    }
}



