package com.booking.show.domain.entity;

/**
 * Lifecycle state of a scheduled show.
 *
 * <ul>
 *   <li>{@code ACTIVE}    — bookings are open.</li>
 *   <li>{@code CANCELLED} — show called off; all bookings must be refunded.</li>
 *   <li>{@code COMPLETED} — show has ended; no further bookings allowed.</li>
 * </ul>
 */
public enum ShowStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED
}
