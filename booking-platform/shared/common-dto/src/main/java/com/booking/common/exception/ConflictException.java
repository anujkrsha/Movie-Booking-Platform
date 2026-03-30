package com.booking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation conflicts with current resource state
 * (e.g. duplicate email, seat already booked).
 * Maps to HTTP 409 Conflict.
 */
public class ConflictException extends BookingException {

    private static final String ERROR_CODE = "CONFLICT";

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, ERROR_CODE, message);
    }
}
