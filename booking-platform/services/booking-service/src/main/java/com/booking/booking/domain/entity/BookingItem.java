package com.booking.booking.domain.entity;

import com.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One seat line-item within a {@link Booking}.
 *
 * <p>{@code seatInventoryId} is a cross-service UUID reference to
 * {@code SeatInventory.id} in show-service (no DB-level FK).
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code booking_id} — retrieve all items for a booking (FK lookup).</li>
 * </ul>
 */
@Entity
@Table(
    name = "booking_items",
    indexes = {
        @Index(name = "idx_booking_item_booking_id", columnList = "booking_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem extends BaseEntity {

    /**
     * Parent booking — proper JPA association; BookingItem and Booking share the same DB.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    /** Cross-service reference to {@code SeatInventory.id} in show-service. */
    @Column(name = "seat_inventory_id", nullable = false, updatable = false)
    private UUID seatInventoryId;

    /** Denormalised seat label (e.g. "A5") — avoids cross-service call at ticket-display time. */
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    /** Face value of this seat at booking time (snapshot; base price before discounts). */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Amount discounted for this specific seat from an applied offer. */
    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;
}
