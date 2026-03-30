package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code payment-events} by payment-service when the gateway returns a failure
 * or the retry budget is exhausted.
 *
 * <p>Consumers:
 * <ul>
 *   <li>booking-service  — transitions booking to FAILED; triggers seat release.</li>
 *   <li>notification-service — notifies the customer of the failed charge.</li>
 * </ul>
 */
@Value
@Builder
@AllArgsConstructor
public class PaymentFailedEvent {

    String eventType = "PaymentFailed";

    UUID bookingId;

    /** Gateway error code or human-readable reason (e.g. "Insufficient funds"). */
    String reason;

    @Builder.Default
    Instant timestamp = Instant.now();
}
