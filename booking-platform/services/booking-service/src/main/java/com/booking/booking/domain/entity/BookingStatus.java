package com.booking.booking.domain.entity;

/**
 * Lifecycle state of a ticket booking.
 *
 * <pre>
 *  PENDING ──► CONFIRMED ──► (end)
 *     │                └──► CANCELLED
 *     ├──► CANCELLED
 *     └──► EXPIRED   (TTL elapsed before payment)
 *          └──► FAILED  (payment gateway returned failure)
 * </pre>
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    FAILED
}
