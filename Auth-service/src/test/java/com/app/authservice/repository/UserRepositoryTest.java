package com.app.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.User;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan(basePackageClasses = User.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void repositoriesSupportLookupAndCountQueries() {
        User user = userRepository.saveAndFlush(User.builder()
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hash")
                .fullName("Jane Doe")
                .role(Role.AUTHOR)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        assertThat(userRepository.findByEmail("jane@example.com")).contains(user);
        assertThat(userRepository.findByUsername("jane")).contains(user);
        assertThat(userRepository.findByUserId(user.getUserId())).contains(user);
        assertThat(userRepository.existsByEmail("jane@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("jane")).isTrue();
        assertThat(userRepository.findAllByRole(Role.AUTHOR)).contains(user);
        assertThat(userRepository.searchByUsername("jan")).contains(user);
        assertThat(userRepository.adminSearchUsers("jane", Role.AUTHOR, true)).contains(user);
        assertThat(userRepository.countByRole(Role.AUTHOR)).isEqualTo(1L);
        assertThat(userRepository.countByRoleAndIsActive(Role.AUTHOR, true)).isEqualTo(1L);
        assertThat(userRepository.countByIsActive(true)).isEqualTo(1L);

        assertThat(userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())).isEmpty();
        assertThat(paymentOrderRepository.findAll()).isEmpty();
        assertThat(emailOtpRepository.findByEmailAndPurposeAndConsumedFalse("jane@example.com", com.app.authservice.entity.OtpPurpose.SIGNUP)).isEmpty();
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 10))).isEmpty();
    }
}

