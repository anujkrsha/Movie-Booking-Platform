package com.booking.movie.application.mapper;

import com.booking.movie.application.dto.MovieResponse;
import com.booking.movie.application.dto.MovieSearchResult;
import com.booking.movie.domain.entity.Movie;
import com.booking.movie.infrastructure.elasticsearch.document.MovieDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper between domain, document, and DTO layers.
 *
 * <p>{@code componentModel = "spring"} makes it a Spring bean injectable everywhere.
 */
@Mapper(componentModel = "spring")
public interface MovieMapper {

    // ── Movie (JPA) → MovieResponse ──────────────────────────────────────────

    MovieResponse toResponse(Movie movie);

    List<MovieResponse> toResponseList(List<Movie> movies);

    // ── Movie (JPA) → MovieDocument (ES) ─────────────────────────────────────

    /**
     * Maps a {@link Movie} entity to an Elasticsearch {@link MovieDocument}.
     *
     * <p>{@code id} is converted from UUID to String by MapStruct's built-in type converter.
     * {@code cities} and {@code showCount} are intentionally ignored here — they are populated
     * by the daily sync scheduler which queries show-service data.
     * {@code status} uses the enum name.
     */
    @Mapping(target = "id",        expression = "java(movie.getId() != null ? movie.getId().toString() : null)")
    @Mapping(target = "status",    expression = "java(movie.getStatus() != null ? movie.getStatus().name() : null)")
    @Mapping(target = "cities",    ignore = true)
    @Mapping(target = "showCount", ignore = true)
    MovieDocument toDocument(Movie movie);

    // ── MovieDocument (ES) → MovieSearchResult ────────────────────────────────

    @Mapping(target = "status", expression = "java(document.getStatus() != null ? com.booking.movie.domain.entity.MovieStatus.valueOf(document.getStatus()) : null)")
    MovieSearchResult toSearchResult(MovieDocument document);

    List<MovieSearchResult> toSearchResultList(List<MovieDocument> documents);
}
