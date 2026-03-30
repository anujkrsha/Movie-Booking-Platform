package com.booking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for invalid input that fails domain-level validation.
 * Maps to HTTP 400 Bad Request.
 */
public class BadRequestException extends BookingException {

    private static final String ERROR_CODE = "BAD_REQUEST";

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, ERROR_CODE, message);
    }
}
