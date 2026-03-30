package com.booking.movie.infrastructure.elasticsearch.query;

import com.booking.movie.domain.entity.MovieStatus;
import co.elastic.clients.json.JsonData;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import java.time.LocalDate;

/**
 * Factory that builds Elasticsearch {@link NativeQuery} objects for movie search.
 *
 * <p>Query structure:
 * <pre>
 * bool:
 *   must:
 *     term { status: RELEASED }
 *   filter: (applied only when the parameter is non-null / non-blank)
 *     term { language: ? }
 *     term { genre: ? }
 *     term { cities: ? }
 *     range { releaseDate: { lte: date } }
 * </pre>
 */
public final class MovieSearchQuery {

    private MovieSearchQuery() {}

    /**
     * Builds a paginated {@link NativeQuery} applying the supplied filters.
     *
     * @param city     city to filter by (matches against the {@code cities} keyword array)
     * @param date     upper bound on {@code releaseDate} — movies released on or before this date
     * @param language exact language filter
     * @param genre    exact genre filter
     * @param page     zero-based page index
     * @param limit    page size
     */
    public static NativeQuery build(
            String city,
            LocalDate date,
            String language,
            String genre,
            int page,
            int limit) {

        Pageable pageable = PageRequest.of(page, limit);

        return NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // Always restrict to RELEASED movies
                            b.must(m -> m
                                    .term(t -> t
                                            .field("status")
                                            .value(MovieStatus.RELEASED.name())));

                            if (language != null && !language.isBlank()) {
                                b.filter(f -> f
                                        .term(t -> t.field("language").value(language)));
                            }

                            if (genre != null && !genre.isBlank()) {
                                b.filter(f -> f
                                        .term(t -> t.field("genre").value(genre)));
                            }

                            if (city != null && !city.isBlank()) {
                                b.filter(f -> f
                                        .term(t -> t.field("cities").value(city)));
                            }

                            if (date != null) {
                                b.filter(f -> f
                                        .range(r -> r
                                                .untyped(u -> u
                                                        .field("releaseDate")
                                                        .lte(JsonData.of(date.toString())))));
                            }

                            return b;
                        }))
                .withPageable(pageable)
                .build();
    }
}
