package com.booking.movie.domain.entity;

import com.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A movie available (or upcoming) on the platform.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code release_date} — upcoming/current shows filter.</li>
 *   <li>{@code genre}        — genre browse / search filter.</li>
 *   <li>{@code language}     — multi-language filter.</li>
 *   <li>{@code status}       — lifecycle status; search filters on RELEASED.</li>
 * </ul>
 */
@Entity
@Table(
    name = "movies",
    indexes = {
        @Index(name = "idx_movie_release_date", columnList = "release_date"),
        @Index(name = "idx_movie_genre",        columnList = "genre"),
        @Index(name = "idx_movie_language",     columnList = "language"),
        @Index(name = "idx_movie_status",       columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "genre", nullable = false, length = 80)
    private String genre;

    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins;

    /** Aggregate audience rating, e.g. 8.5 out of 10. Stored with 2 decimal places. */
    @Column(name = "rating", precision = 4, scale = 2)
    private BigDecimal rating;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "poster_url", length = 1024)
    private String posterUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MovieStatus status = MovieStatus.UPCOMING;
}
