package com.booking.movie.web.controller;

import com.booking.common.dto.ApiResponse;
import com.booking.common.dto.PagedResponse;
import com.booking.movie.application.dto.CreateMovieRequest;
import com.booking.movie.application.dto.MovieResponse;
import com.booking.movie.application.dto.MovieSearchResult;
import com.booking.movie.application.dto.UpdateMovieRequest;
import com.booking.movie.application.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Movie catalogue endpoints.
 *
 * <pre>
 *  GET  /v1/movies/search   — public, Elasticsearch-backed
 *  GET  /v1/movies/{id}     — public, Redis-cached
 *  POST /v1/movies          — PLATFORM_ADMIN only
 *  PUT  /v1/movies/{id}     — PLATFORM_ADMIN only
 * </pre>
 */
@RestController
@RequestMapping("/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movies", description = "Movie catalogue — search, details, and admin management")
public class MovieController {

    private final MovieService movieService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @Operation(summary = "Search movies",
               description = "Queries Elasticsearch for RELEASED movies with optional filters. " +
                             "Returns paginated results with show count per movie.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<MovieSearchResult>> searchMovies(
            @Parameter(description = "Filter by city name (must match a city where the movie is showing)")
            @RequestParam(required = false) String city,

            @Parameter(description = "Show movies released on or before this date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Filter by language (exact match)")
            @RequestParam(required = false) String language,

            @Parameter(description = "Filter by genre (exact match)")
            @RequestParam(required = false) String genre,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        return ResponseEntity.ok(
                movieService.searchMovies(city, date, language, genre, page, limit));
    }

    @Operation(summary = "Get movie by ID",
               description = "Returns movie details. Response is cached in Redis for 1 hour.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Movie retrieved successfully", movieService.getById(id)));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @Operation(summary = "Create a new movie (PLATFORM_ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movie created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
            @Valid @RequestBody CreateMovieRequest req) {

        MovieResponse created = movieService.createMovie(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", created));
    }

    @Operation(summary = "Update an existing movie (PLATFORM_ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMovieRequest req) {

        MovieResponse updated = movieService.updateMovie(id, req);
        return ResponseEntity.ok(
                ApiResponse.success("Movie updated successfully", updated));
    }
}
