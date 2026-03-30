package com.booking.offer.domain.entity;

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
import java.time.LocalDate;

/**
 * A promotional discount offer applied during the booking checkout flow.
 *
 * <p>JSON fields:
 * <ul>
 *   <li>{@code condition}         — type-specific trigger rules (see {@link OfferType}).</li>
 *   <li>{@code applicableCities}  — JSON array of city names where the offer is valid,
 *       e.g. {@code ["Mumbai","Delhi","Bangalore"]}. Empty array means all cities.</li>
 * </ul>
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code active}            — fast filter for live offers.</li>
 *   <li>{@code type}              — query offers of a specific type.</li>
 *   <li>{@code start_date, end_date} — validity window queries.</li>
 * </ul>
 */
@Entity
@Table(
    name = "offers",
    indexes = {
        @Index(name = "idx_offer_active",     columnList = "active"),
        @Index(name = "idx_offer_type",       columnList = "type"),
        @Index(name = "idx_offer_validity",   columnList = "start_date, end_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OfferType type;

    /**
     * Type-specific trigger condition as a JSON object.
     * Examples:
     * <ul>
     *   <li>BULK_DISCOUNT: {@code {"minSeats": 4}}</li>
     *   <li>TIME_DISCOUNT:  {@code {"beforeHour": 12}}</li>
     * </ul>
     */
    @Column(name = "condition", nullable = false, columnDefinition = "text")
    private String condition;

    /** Percentage discount to apply, e.g. {@code 15.00} for 15 %. */
    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * JSON array of city names this offer applies to.
     * {@code []} or {@code null} means the offer is valid in all cities.
     */
    @Column(name = "applicable_cities", columnDefinition = "text")
    private String applicableCities;

    @Column(name = "active", nullable = false)
    private boolean active;
}
