package com.booking.show.domain.entity;

/**
 * Availability state of a single seat within a show.
 *
 * <ul>
 *   <li>{@code AVAILABLE} — can be selected by a customer.</li>
 *   <li>{@code LOCKED}    — held in-session for a customer completing checkout;
 *                           expires after the TTL defined in {@code show.seat-lock.ttl-minutes}.</li>
 *   <li>{@code BOOKED}    — payment confirmed; seat is permanently reserved.</li>
 * </ul>
 */
public enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
}
