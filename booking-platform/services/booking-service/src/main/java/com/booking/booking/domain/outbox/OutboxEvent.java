package com.booking.booking.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional outbox record written in the same DB transaction as the business entity.
 *
 * <p>The outbox pattern guarantees at-least-once Kafka delivery even if the JVM crashes
 * between the DB commit and the {@code kafkaTemplate.send()} call. The relay polls this
 * table and publishes events, then marks them as PROCESSED.
 *
 * <p>Does NOT extend {@link com.booking.common.entity.BaseEntity} — the outbox has its
 * own minimal timestamp model and does not need {@code updatedAt}.
 *
 * <p>Table indexes:
 * <ul>
 *   <li>{@code status, created_at} — the relay's primary poll query.</li>
 *   <li>{@code aggregate_id}       — look up all outbox events for a given booking.</li>
 * </ul>
 */
@Entity
@Table(
    name = "booking_outbox",
    indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_outbox_aggregate_id",   columnList = "aggregate_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID of the aggregate this event belongs to — typically the {@code Booking.id}.
     * Also used as the Kafka message key, which routes all events for the same booking
     * to the same partition and preserves ordering.
     */
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    /**
     * Domain aggregate type (e.g. {@code "Booking"}).
     * Useful for multi-aggregate outbox tables and DLQ analysis.
     */
    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 60)
    private String aggregateType;

    /**
     * Fully qualified event class simple name (e.g. {@code "BookingInitiatedEvent"}).
     * The relay uses this as the Kafka message header for consumer-side routing.
     */
    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    /**
     * JSON-serialised event payload. The relay deserialises this and publishes it to Kafka
     * as the message value.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    /**
     * Kafka topic to publish this event to (e.g. {@code "booking-events"}).
     * Stored here so the relay doesn't need topic-routing logic.
     */
    @Column(name = "topic", nullable = false, updatable = false, length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    /** Number of Kafka publish attempts made so far. */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Set by the relay when publishing succeeds or the retry budget is exhausted. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
