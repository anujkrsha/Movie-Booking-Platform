package com.booking.common.infra.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Defines all platform Kafka topics as Spring-managed {@link NewTopic} beans.
 *
 * <p>Spring Kafka's {@code KafkaAdmin} picks up every {@link NewTopic} bean in the context
 * and issues idempotent {@code CreateTopics} calls on startup — safe to run from any service.
 *
 * <p>Topic design rationale:
 * <ul>
 *   <li><b>3 partitions</b> — supports 3 parallel consumer instances per group in production;
 *       keep local at 1 replica (replication-factor=1 in docker-compose).</li>
 *   <li><b>booking-events</b>   — booking lifecycle: INITIATED → CONFIRMED / FAILED.</li>
 *   <li><b>payment-events</b>   — payment gateway results consumed by booking-service.</li>
 *   <li><b>notify-events</b>    — notification triggers consumed by notification-service.</li>
 *   <li><b>analytics-events</b> — denormalised event stream consumed by search/reporting.</li>
 *   <li><b>*.DLT</b>            — dead-letter topics; created by the error handler automatically
 *       but declared here so retention/alert rules can be applied from day one.</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(NewTopic.class)
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;   // 1 for local dev; override to 3 in production

    // ── Primary topics ────────────────────────────────────────────────────────

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(KafkaTopics.BOOKING_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notifyEventsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFY_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ANALYTICS_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    // ── Dead-letter topics ────────────────────────────────────────────────────
    // Declared explicitly so we can set longer retention for offline debugging.

    @Bean
    public NewTopic bookingEventsDlt() {
        return TopicBuilder.name(KafkaTopics.BOOKING_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000)) // 7 days
                .build();
    }

    @Bean
    public NewTopic paymentEventsDlt() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }

    @Bean
    public NewTopic notifyEventsDlt() {
        return TopicBuilder.name(KafkaTopics.NOTIFY_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }

    @Bean
    public NewTopic analyticsEventsDlt() {
        return TopicBuilder.name(KafkaTopics.ANALYTICS_EVENTS + ".DLT")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }
}
