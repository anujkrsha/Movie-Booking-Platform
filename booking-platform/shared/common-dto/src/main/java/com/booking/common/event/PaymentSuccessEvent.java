package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code payment-events} by payment-service after the gateway confirms success.
 *
 * <p>Consumers:
 * <ul>
 *   <li>booking-service  — transitions booking from PENDING → CONFIRMED.</li>
 *   <li>analytics-events — forwarded for revenue reporting.</li>
 * </ul>
 */
@Value
@Builder
@AllArgsConstructor
public class PaymentSuccessEvent {

    String eventType = "PaymentSuccess";

    UUID bookingId;

    /** Gateway's own transaction identifier for reconciliation. */
    String gatewayTxnId;

    BigDecimal amount;

    @Builder.Default
    Instant timestamp = Instant.now();
}
