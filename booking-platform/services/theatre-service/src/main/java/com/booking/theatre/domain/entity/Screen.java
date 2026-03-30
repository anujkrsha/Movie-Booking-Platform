package com.booking.theatre.domain.entity;

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

/**
 * An individual auditorium (screen) inside a {@link Theatre}.
 *
 * <p>{@code seatLayout} is a JSON string describing the physical seat grid,
 * e.g. {@code {"rows":[{"label":"A","seats":["A1","A2",...]},...]}}.
 * It is stored as {@code text} and deserialised by the service layer on demand.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code theatre_id} — list all screens for a theatre (FK lookup).</li>
 * </ul>
 */
@Entity
@Table(
    name = "screens",
    indexes = {
        @Index(name = "idx_screen_theatre_id", columnList = "theatre_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen extends BaseEntity {

    /**
     * Owning theatre — modelled as a proper JPA association because Screen and
     * Theatre live in the same service and the same database.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theatre_id", nullable = false, updatable = false)
    private Theatre theatre;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    /**
     * JSON layout descriptor.
     * Example: {@code {"rows":[{"label":"A","seats":["A1","A2"]}]}}
     */
    @Column(name = "seat_layout", nullable = false, columnDefinition = "text")
    private String seatLayout;
}
