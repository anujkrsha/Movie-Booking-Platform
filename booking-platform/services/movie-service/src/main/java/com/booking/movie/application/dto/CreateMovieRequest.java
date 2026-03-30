package com.booking.movie.application.dto;

import com.booking.movie.domain.entity.MovieStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for {@code POST /v1/movies}. */
@Data
public class CreateMovieRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 50)
    private String language;

    @NotBlank
    @Size(max = 80)
    private String genre;

    @NotNull
    @Positive
    private Integer durationMins;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal rating;

    @NotNull
    private LocalDate releaseDate;

    @Size(max = 1024)
    private String posterUrl;

    private String description;

    /** Defaults to {@code UPCOMING} if omitted. */
    private MovieStatus status;
}
