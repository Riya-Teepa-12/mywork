package com.app.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuth2AuthenticationFailureHandlerTest {

    @Test
    void redirectsToFrontendLoginWithError() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                org.mockito.Mockito.mock(HttpServletRequest.class),
                response,
                new BadCredentialsException("provider denied"));

        assertThat(response.getRedirectedUrl()).contains("http://localhost:5173/login");
        assertThat(response.getRedirectedUrl()).contains("oauthError=provider denied");
    }

    @Test
    void usesDefaultMessageWhenExceptionMessageMissing() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                org.mockito.Mockito.mock(HttpServletRequest.class),
                response,
                new BadCredentialsException(" "));

        assertThat(response.getRedirectedUrl()).contains("oauthError=OAuth login failed");
    }
}
