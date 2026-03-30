package com.booking.common.infra.kafka;

/**
 * Central registry of Kafka topic name constants.
 *
 * <p>Use these in {@code @KafkaListener(topics = KafkaTopics.BOOKING_EVENTS)} and
 * {@code kafkaTemplate.send(KafkaTopics.BOOKING_EVENTS, ...)} instead of raw strings.
 */
public final class KafkaTopics {

    public static final String BOOKING_EVENTS  = "booking-events";
    public static final String PAYMENT_EVENTS  = "payment-events";
    public static final String NOTIFY_EVENTS   = "notify-events";
    public static final String ANALYTICS_EVENTS = "analytics-events";

    private KafkaTopics() {
        // constants-only class
    }
}
