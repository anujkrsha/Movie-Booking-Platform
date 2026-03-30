package com.booking.booking.domain.outbox;

/**
 * Lifecycle state of an outbox event record.
 *
 * <pre>
 *  PENDING ──► PROCESSED
 *     └──────► FAILED   (retry_count ≥ MAX_RETRIES)
 * </pre>
 */
public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
