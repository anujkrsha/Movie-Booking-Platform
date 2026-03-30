package com.booking.show.domain.entity;

import com.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One seat in the inventory of a particular {@link Show}.
 *
 * <p>Optimistic locking via {@link Version} prevents double-booking under concurrent
 * checkout requests — the seat-lock service increments the version on LOCK and the
 * booking service on BOOKED; a stale-state exception triggers a retry.
 *
 * <p>Constraints:
 * <ul>
 *   <li>Unique constraint on {@code (show_id, seat_number)} — a seat can appear only once per show.</li>
 * </ul>
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code show_id}        — all seats for a show (FK lookup).</li>
 *   <li>{@code status}         — fast AVAILABLE seat count / filter.</li>
 *   <li>{@code show_id, status} — composite: available seats for a specific show.</li>
 * </ul>
 */
@Entity
@Table(
    name = "seat_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_seat_show_number", columnNames = {"show_id", "seat_number"})
    },
    indexes = {
        @Index(name = "idx_seat_inv_show_id",        columnList = "show_id"),
        @Index(name = "idx_seat_inv_status",          columnList = "status"),
        @Index(name = "idx_seat_inv_show_status",     columnList = "show_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInventory extends BaseEntity {

    /**
     * Owning show — proper JPA association; SeatInventory and Show live in the same service/DB.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false, updatable = false)
    private Show show;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    /** Row label as displayed in the seat map (e.g. "A", "B", "12"). */
    @Column(name = "row", nullable = false, length = 10)
    private String row;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SeatCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    /** Effective price for this seat (may differ from base show pricing after discounts). */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Hibernate optimistic-lock version counter.
     * Incremented automatically on every UPDATE; a stale read raises
     * {@link jakarta.persistence.OptimisticLockException}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
