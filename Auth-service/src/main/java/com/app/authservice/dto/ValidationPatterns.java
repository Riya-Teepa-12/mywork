package com.app.authservice.dto;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String STRONG_CREDENTIAL_REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,120}$";
}
