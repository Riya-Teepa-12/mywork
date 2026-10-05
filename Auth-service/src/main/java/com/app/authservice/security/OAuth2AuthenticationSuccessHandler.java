package com.app.authservice.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.authservice.dto.AuthResponse;
import com.app.authservice.dto.OAuthLoginRequest;
import com.app.authservice.entity.AuthProvider;
import com.app.authservice.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${inkwell.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            String email = stringValue(user.getAttribute("email"));

            if (email == null || email.isBlank()) {
                if ("github".equalsIgnoreCase(registrationId)) {
                    email = fetchGithubEmail(oauthToken);
                }
            }

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email not available from OAuth provider");
            }
            String emailLocalPart = localPartFromEmail(email);

            String fullName = firstNonBlank(
                    stringValue(user.getAttribute("name")),
                    stringValue(user.getAttribute("login")),
                    emailLocalPart);

            String username = sanitizeUsername(firstNonBlank(
                    stringValue(user.getAttribute("login")),
                    emailLocalPart,
                    fullName));

            String avatarUrl = firstNonBlank(
                    stringValue(user.getAttribute("picture")),
                    stringValue(user.getAttribute("avatar_url")));

            OAuthLoginRequest oauthRequest = new OAuthLoginRequest();
            oauthRequest.setProvider("github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE);
            oauthRequest.setEmail(email.trim().toLowerCase(Locale.ROOT));
            oauthRequest.setFullName(fullName);
            oauthRequest.setUsername(username);
            oauthRequest.setAvatarUrl(avatarUrl);

            AuthResponse authResponse = authService.oauthLogin(oauthRequest);
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(trimTrailingSlash(frontendUrl) + "/oauth/callback")
                    .queryParam("token", authResponse.getAccessToken())
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(trimTrailingSlash(frontendUrl) + "/login")
                    .queryParam("oauthError", ex.getMessage())
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String sanitizeUsername(String value) {
        String cleaned = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (cleaned.isBlank()) {
            cleaned = "user";
        }
        if (cleaned.length() < 3) {
            cleaned = cleaned + "user";
        }
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60);
        }
        return cleaned + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String localPartFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return "user";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        return email.substring(0, atIndex);
    }

    private String fetchGithubEmail(OAuth2AuthenticationToken oauthToken) {
        try {
            OAuth2AuthorizedClient client =
                    authorizedClientService.loadAuthorizedClient(
                            oauthToken.getAuthorizedClientRegistrationId(),
                            oauthToken.getName()
                    );
            if (client == null || client.getAccessToken() == null || client.getAccessToken().getTokenValue() == null) {
                return null;
            }

            String accessToken = client.getAccessToken().getTokenValue();
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    List.class
            );
            List<Map<String, Object>> emails = response.getBody();
            if (emails == null || emails.isEmpty()) {
                return null;
            }

            String primary = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                    .map(email -> stringValue(email.get("email")))
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
            if (primary != null) {
                return primary;
            }

            String verified = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> stringValue(email.get("email")))
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
            if (verified != null) {
                return verified;
            }

            return emails.stream()
                    .map(email -> stringValue(email.get("email")))
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
