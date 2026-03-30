package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code booking-events} when a booking reaches CONFIRMED state after
 * successful payment.
 *
 * <p>Consumers:
 * <ul>
 *   <li>notification-service — sends confirmation email/SMS with QR code.</li>
 *   <li>analytics-events     — forwarded for revenue reporting.</li>
 * </ul>
 */
@Value
@Builder
@AllArgsConstructor
public class BookingConfirmedEvent {

    String eventType = "BookingConfirmed";

    UUID bookingId;

    /** Human-readable ticket reference shown on the confirmation screen. */
    String confirmationNumber;

    @Builder.Default
    Instant timestamp = Instant.now();
}
