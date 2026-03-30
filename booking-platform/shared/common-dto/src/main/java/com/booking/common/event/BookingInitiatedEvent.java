package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published to {@code booking-events} when a booking enters PENDING state and seats are locked.
 *
 * <p>Consumers:
 * <ul>
 *   <li>payment-service  — initiates the payment charge.</li>
 *   <li>analytics-events — forwarded for real-time reporting.</li>
 * </ul>
 */
@Value
@Builder
@AllArgsConstructor
public class BookingInitiatedEvent {

    String eventType = "BookingInitiated";

    UUID bookingId;

    /** User who triggered the booking. */
    UUID userId;

    UUID showId;

    /** Seat numbers selected (e.g. ["A1","A2"]). */
    List<String> seats;

    /** Gross amount before any discount. */
    BigDecimal totalAmount;

    @Builder.Default
    Instant timestamp = Instant.now();
}
