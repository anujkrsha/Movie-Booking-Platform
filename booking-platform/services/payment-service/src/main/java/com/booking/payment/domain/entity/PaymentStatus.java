package com.booking.payment.domain.entity;

/**
 * Lifecycle state of a payment transaction.
 *
 * <pre>
 *  INITIATED ──► SUCCESS
 *      │    └──► FAILED ──► (retry) ──► SUCCESS | FAILED
 *      └──► REFUNDED  (post-cancellation)
 * </pre>
 */
public enum PaymentStatus {
    INITIATED,
    SUCCESS,
    FAILED,
    REFUNDED
}
