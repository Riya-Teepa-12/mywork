package com.app.authservice.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${inkwell.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String message = exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                ? "OAuth login failed"
                : exception.getMessage();
        String redirectUrl = UriComponentsBuilder
                .fromUriString(trimTrailingSlash(frontendUrl) + "/login")
                .queryParam("oauthError", message)
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

