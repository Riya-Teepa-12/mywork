package com.app.commentservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Map<String, String> body = handler.handleIllegalArgument(new IllegalArgumentException("bad input")).getBody();

        assertThat(handler.handleIllegalArgument(new IllegalArgumentException("bad input")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).containsEntry("message", "bad input");
    }

    @Test
    void handleValidationReturnsFieldMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException ex = createValidationException("content", "must not be blank");

        Map<String, String> body = handler.handleValidation(ex).getBody();

        assertThat(body).containsEntry("message", "content must not be blank");
    }

    private MethodArgumentNotValidException createValidationException(String field, String message) {
        try {
            Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
            MethodParameter parameter = new MethodParameter(method, 0);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", field, message));
            return new MethodArgumentNotValidException(parameter, bindingResult);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unused")
    private static void sample(String value) {
        // Reflection target only; this method must not be executed in test flow.
        throw new UnsupportedOperationException("Test helper method");
    }
}
