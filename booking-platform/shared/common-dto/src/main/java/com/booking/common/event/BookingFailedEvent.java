package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code booking-events} when a booking moves to FAILED or EXPIRED state.
 *
 * <p>Consumers:
 * <ul>
 *   <li>show-service         — releases the locked seats back to AVAILABLE.</li>
 *   <li>notification-service — sends failure/expiry notification to the customer.</li>
 * </ul>
 */
@Value
@Builder
@AllArgsConstructor
public class BookingFailedEvent {

    String eventType = "BookingFailed";

    UUID bookingId;

    /** Human-readable failure reason (e.g. "Payment declined", "Session expired"). */
    String reason;

    @Builder.Default
    Instant timestamp = Instant.now();
}
