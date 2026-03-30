package com.booking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for all domain-level errors in the booking platform.
 * Carries an {@link HttpStatus} so the global handler can set the response code
 * without needing a separate mapping table.
 */
@Getter
public class BookingException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BookingException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BookingException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
