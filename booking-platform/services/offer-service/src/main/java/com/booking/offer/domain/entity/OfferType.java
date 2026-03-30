package com.booking.offer.domain.entity;

/**
 * Classification of an offer's discount trigger.
 *
 * <ul>
 *   <li>{@code BULK_DISCOUNT} — discount applied when a minimum seat count is reached
 *       (condition JSON: {@code {"minSeats": 4}}).</li>
 *   <li>{@code TIME_DISCOUNT}  — discount applied within a specific time window
 *       (condition JSON: {@code {"beforeHour": 12}}).</li>
 * </ul>
 */
public enum OfferType {
    BULK_DISCOUNT,
    TIME_DISCOUNT
}
