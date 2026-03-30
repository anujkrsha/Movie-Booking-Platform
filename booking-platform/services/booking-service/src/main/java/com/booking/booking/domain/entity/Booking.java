package com.booking.booking.domain.entity;

import com.booking.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A customer's ticket booking for a show.
 *
 * <p>Cross-service UUID references:
 * <ul>
 *   <li>{@code userId}    → {@code User.id}    in user-service.</li>
 *   <li>{@code showId}    → {@code Show.id}    in show-service.</li>
 *   <li>{@code paymentId} → {@code Payment.id} in payment-service (set after payment completes).</li>
 * </ul>
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code user_id}             — all bookings for a customer.</li>
 *   <li>{@code show_id}             — all bookings for a show (admin/refund flows).</li>
 *   <li>{@code status}              — filter by lifecycle state.</li>
 *   <li>{@code confirmation_number} — unique; used for QR-code and support lookup.</li>
 * </ul>
 */
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_user_id",             columnList = "user_id"),
        @Index(name = "idx_booking_show_id",             columnList = "show_id"),
        @Index(name = "idx_booking_status",              columnList = "status"),
        @Index(name = "idx_booking_confirmation_number", columnList = "confirmation_number", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    /** Cross-service reference to {@code User.id} in user-service. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Cross-service reference to {@code Show.id} in show-service. */
    @Column(name = "show_id", nullable = false, updatable = false)
    private UUID showId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** Cross-service reference to {@code Payment.id} in payment-service; null until payment is initiated. */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** Human-readable booking reference shown on ticket (e.g. "BKG-20240501-XYZABC"). */
    @Column(name = "confirmation_number", unique = true, length = 30)
    private String confirmationNumber;

    @Builder.Default
    @OneToMany(
        mappedBy = "booking",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<BookingItem> items = new ArrayList<>();
}
