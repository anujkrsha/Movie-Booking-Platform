package com.booking.movie.application.dto;

import com.booking.movie.domain.entity.MovieStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code PUT /v1/movies/{id}}.
 *
 * <p>All fields are optional — only non-null values are applied (patch semantics).
 */
@Data
public class UpdateMovieRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 50)
    private String language;

    @Size(max = 80)
    private String genre;

    @Positive
    private Integer durationMins;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal rating;

    private LocalDate releaseDate;

    @Size(max = 1024)
    private String posterUrl;

    private String description;

    private MovieStatus status;
}
