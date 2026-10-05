package com.app.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.authservice.security.JwtAuthenticationFilter;
import com.app.authservice.security.OAuth2AuthenticationFailureHandler;
import com.app.authservice.security.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/register",
                        "/auth/register/request-otp",
                        "/auth/register/verify-otp",
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/oauth/login",
                        "/auth/password/forgot/request-otp",
                        "/auth/password/forgot/reset",
                        "/auth/subscriptions/plans",
                        "/auth/internal/entitlements/**")
                .permitAll()
                .requestMatchers("/auth/v3/api-docs").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/public/users/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/validate").permitAll()
                .anyRequest().authenticated());
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2AuthenticationSuccessHandler)
                .failureHandler(oauth2AuthenticationFailureHandler));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

}
