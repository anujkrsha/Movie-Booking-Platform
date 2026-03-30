package com.booking.show.domain.entity;

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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A scheduled screening of a movie in a particular screen.
 *
 * <p>{@code screenId} references {@code Screen} in theatre-service (cross-service UUID reference;
 * no DB-level FK). {@code movieId} references {@code Movie} in movie-service similarly.
 *
 * <p>{@code pricing} is a JSON map of {@link SeatCategory} → price,
 * e.g. {@code {"SILVER":150.00,"GOLD":220.00,"PLATINUM":350.00}}.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code screen_id}        — shows for a given screen.</li>
 *   <li>{@code movie_id}         — shows for a given movie.</li>
 *   <li>{@code date}             — date-based show listing.</li>
 *   <li>{@code status}           — filter active shows quickly.</li>
 *   <li>{@code screen_id, date}  — composite: all shows on a screen for a date.</li>
 * </ul>
 */
@Entity
@Table(
    name = "shows",
    indexes = {
        @Index(name = "idx_show_screen_id",        columnList = "screen_id"),
        @Index(name = "idx_show_movie_id",         columnList = "movie_id"),
        @Index(name = "idx_show_date",             columnList = "date"),
        @Index(name = "idx_show_status",           columnList = "status"),
        @Index(name = "idx_show_screen_date",      columnList = "screen_id, date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show extends BaseEntity {

    /** Cross-service reference to {@code Screen.id} in theatre-service. */
    @Column(name = "screen_id", nullable = false, updatable = false)
    private UUID screenId;

    /** Cross-service reference to {@code Movie.id} in movie-service. */
    @Column(name = "movie_id", nullable = false, updatable = false)
    private UUID movieId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * JSON map: SeatCategory → base price (BigDecimal).
     * Example: {@code {"SILVER":150.00,"GOLD":220.00,"PLATINUM":350.00}}
     */
    @Column(name = "pricing", nullable = false, columnDefinition = "text")
    private String pricing;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShowStatus status;

    /** Decremented atomically as seats are booked; used for quick availability checks. */
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
}
