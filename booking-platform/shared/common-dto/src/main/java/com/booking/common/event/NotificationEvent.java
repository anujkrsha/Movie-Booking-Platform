package com.booking.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Published to {@code notify-events} by any service that needs to send a user-facing message.
 * Consumed exclusively by notification-service, which routes to the appropriate channels.
 *
 * <p>Channel values: {@code "EMAIL"}, {@code "SMS"}, {@code "PUSH"}.
 *
 * <p>Example:
 * <pre>{@code
 * NotificationEvent.builder()
 *     .userId(bookingConfirmedEvent.getBookingId()) // resolved to userId upstream
 *     .type("BOOKING_CONFIRMED")
 *     .subject("Your booking is confirmed!")
 *     .body("Booking ref: BKG-20240601-ABCXYZ")
 *     .channels(List.of("EMAIL", "SMS"))
 *     .build();
 * }</pre>
 */
@Value
@Builder
@AllArgsConstructor
public class NotificationEvent {

    String eventType = "Notification";

    /** Recipient user ID — notification-service resolves email/phone from user-service. */
    UUID userId;

    /**
     * Semantic notification type used for template selection
     * (e.g. {@code "BOOKING_CONFIRMED"}, {@code "PAYMENT_FAILED"}).
     */
    String type;

    /** Email subject line (ignored for SMS/PUSH channels). */
    String subject;

    /** Plain-text or HTML body of the message. */
    String body;

    /** Delivery channels requested by the publisher. */
    List<String> channels;
}
