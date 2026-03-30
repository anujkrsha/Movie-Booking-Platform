package com.booking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity does not exist in the data store.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BookingException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(HttpStatus.NOT_FOUND, ERROR_CODE,
                String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ERROR_CODE, message);
    }
}
