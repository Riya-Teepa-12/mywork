package com.app.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.authservice.dto.AuthResponse;
import com.app.authservice.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Test
    void redirectsToFrontendCallbackOnSuccess() throws Exception {
        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService, authorizedClientService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173/");

        OAuth2AuthenticationToken authentication = org.mockito.Mockito.mock(OAuth2AuthenticationToken.class);
        OAuth2User user = org.mockito.Mockito.mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(user.getAttribute("email")).thenReturn("reader@example.com");
        when(user.getAttribute("name")).thenReturn("Reader Name");
        when(user.getAttribute("login")).thenReturn("reader");
        when(user.getAttribute("picture")).thenReturn("http://avatar");
        when(authService.oauthLogin(any())).thenReturn(AuthResponse.builder().accessToken("token-123").build());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(org.mockito.Mockito.mock(HttpServletRequest.class), response, authentication);

        assertThat(response.getRedirectedUrl()).contains("http://localhost:5173/oauth/callback");
        assertThat(response.getRedirectedUrl()).contains("token=token-123");
    }

    @Test
    void redirectsToLoginOnFailure() throws Exception {
        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService, authorizedClientService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173");

        OAuth2AuthenticationToken authentication = org.mockito.Mockito.mock(OAuth2AuthenticationToken.class);
        OAuth2User user = org.mockito.Mockito.mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(user.getAttribute("email")).thenReturn("reader@example.com");
        when(user.getAttribute("name")).thenReturn("Reader Name");
        when(user.getAttribute("login")).thenReturn("reader");
        when(authService.oauthLogin(any())).thenThrow(new IllegalArgumentException("oauth failed"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(org.mockito.Mockito.mock(HttpServletRequest.class), response, authentication);

        assertThat(response.getRedirectedUrl()).contains("http://localhost:5173/login");
        assertThat(response.getRedirectedUrl()).contains("oauthError=oauth failed");
    }
}
