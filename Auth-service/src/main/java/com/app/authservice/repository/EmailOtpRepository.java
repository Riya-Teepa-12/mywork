package com.app.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.authservice.entity.EmailOtp;
import com.app.authservice.entity.OtpPurpose;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);
    List<EmailOtp> findByEmailAndPurposeAndConsumedFalse(String email, OtpPurpose purpose);
}
