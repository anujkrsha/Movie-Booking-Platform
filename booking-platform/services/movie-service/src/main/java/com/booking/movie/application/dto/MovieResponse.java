package com.booking.movie.application.dto;

import com.booking.movie.domain.entity.MovieStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for a single movie (getById, createMovie, updateMovie). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private UUID           id;
    private String         title;
    private String         language;
    private String         genre;

    @JsonProperty("duration_mins")
    private Integer        durationMins;

    private BigDecimal     rating;

    @JsonProperty("release_date")
    private LocalDate      releaseDate;

    @JsonProperty("poster_url")
    private String         posterUrl;

    private String         description;
    private MovieStatus    status;

    @JsonProperty("created_at")
    private LocalDateTime  createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime  updatedAt;
}
