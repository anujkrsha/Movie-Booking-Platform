package com.booking.user.domain.exception;

import com.booking.common.exception.BookingException;
import org.springframework.http.HttpStatus;

/**
 * Thrown for authentication failures: invalid credentials, expired/revoked tokens,
 * or any other condition that should return HTTP 401 Unauthorized.
 *
 * <p>The {@link com.booking.common.exception.GlobalExceptionHandler} handles this via
 * the {@code BookingException} handler — it reads the {@link HttpStatus} carried here
 * and sets the response code accordingly.
 */
public class AuthException extends BookingException {

    private static final String ERROR_CODE = "AUTHENTICATION_FAILED";

    public AuthException(String message) {
        super(HttpStatus.UNAUTHORIZED, ERROR_CODE, message);
    }

    public static AuthException invalidCredentials() {
        return new AuthException("Invalid email or password");
    }

    public static AuthException tokenExpired() {
        return new AuthException("Token has expired");
    }

    public static AuthException tokenInvalid() {
        return new AuthException("Token is invalid or has been revoked");
    }

    public static AuthException refreshTokenInvalid() {
        return new AuthException("Refresh token is invalid, expired, or has already been used");
    }
}
