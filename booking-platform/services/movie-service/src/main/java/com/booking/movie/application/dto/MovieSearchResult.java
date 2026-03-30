package com.booking.movie.application.dto;

import com.booking.movie.domain.entity.MovieStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Search result DTO returned by the Elasticsearch-backed movie search endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResult {

    private String         id;
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

    /** Cities where this movie is currently showing. */
    private List<String>   cities;

    /** Total active shows scheduled; updated daily by the sync scheduler. */
    @JsonProperty("show_count")
    private int            showCount;
}
