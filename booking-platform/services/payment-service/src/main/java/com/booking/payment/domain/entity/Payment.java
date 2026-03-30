package com.booking.payment.domain.entity;

import com.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A payment transaction initiated for a booking.
 *
 * <p>{@code bookingId} is a cross-service UUID reference to {@code Booking.id}
 * in booking-service (no DB-level FK).
 *
 * <p>{@code idempotencyKey} enables safe retries — the payment service checks for an
 * existing record with the same key before forwarding to the gateway, preventing
 * duplicate charges on network timeouts.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code booking_id}      — look up payment for a given booking.</li>
 *   <li>{@code idempotency_key} — unique; de-duplication on retry.</li>
 *   <li>{@code gateway_txn_id}  — reconciliation with the payment gateway dashboard.</li>
 *   <li>{@code status}          — filter failed/refunded payments for ops dashboards.</li>
 * </ul>
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_booking_id",      columnList = "booking_id"),
        @Index(name = "idx_payment_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payment_gateway_txn_id",  columnList = "gateway_txn_id"),
        @Index(name = "idx_payment_status",          columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    /** Cross-service reference to {@code Booking.id} in booking-service. */
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Payment gateway identifier (e.g. "razorpay", "stripe"). */
    @Column(name = "gateway", nullable = false, length = 50)
    private String gateway;

    /** Transaction ID returned by the payment gateway after initiation. */
    @Column(name = "gateway_txn_id", length = 128)
    private String gatewayTxnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * Client-supplied idempotency key (UUID or hash) for safe retries.
     * Unique constraint ensures only one payment record per key.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    /** Number of gateway call attempts made so far; used by retry backoff logic. */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
}
