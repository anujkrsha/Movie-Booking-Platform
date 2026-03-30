package com.booking.movie.application.service;

import com.booking.common.dto.PagedResponse;
import com.booking.common.exception.ResourceNotFoundException;
import com.booking.movie.application.dto.CreateMovieRequest;
import com.booking.movie.application.dto.MovieResponse;
import com.booking.movie.application.dto.MovieSearchResult;
import com.booking.movie.application.dto.UpdateMovieRequest;
import com.booking.movie.application.mapper.MovieMapper;
import com.booking.movie.domain.entity.Movie;
import com.booking.movie.domain.entity.MovieStatus;
import com.booking.movie.domain.event.MovieCreatedEvent;
import com.booking.movie.domain.repository.MovieRepository;
import com.booking.movie.infrastructure.elasticsearch.document.MovieDocument;
import com.booking.movie.infrastructure.elasticsearch.query.MovieSearchQuery;
import com.booking.movie.infrastructure.elasticsearch.repository.MovieElasticsearchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core service for the movie catalogue.
 *
 * <h3>Caching strategy</h3>
 * <p>{@link #getById(UUID)} checks Redis under the key {@code MOVIE:{id}} with a 1-hour TTL
 * before falling back to PostgreSQL.  {@link #updateMovie} evicts the key on every write.
 *
 * <h3>Search strategy</h3>
 * <p>{@link #searchMovies} builds a {@code bool} query via {@link MovieSearchQuery} and
 * executes it against Elasticsearch using {@link ElasticsearchOperations}.
 *
 * <h3>Indexing strategy</h3>
 * <p>{@link #createMovie} publishes a {@link MovieCreatedEvent}; a
 * {@link com.booking.movie.infrastructure.elasticsearch.sync.MovieSyncScheduler TransactionalEventListener}
 * picks it up after commit and indexes the document.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private static final String CACHE_PREFIX = "MOVIE:";

    private final MovieRepository               movieRepository;
    private final MovieElasticsearchRepository  esRepository;
    private final ElasticsearchOperations       elasticsearchOperations;
    private final MovieMapper                   movieMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;
    private final ApplicationEventPublisher     eventPublisher;

    // ── Search ───────────────────────────────────────────────────────────────

    /**
     * Queries Elasticsearch for RELEASED movies matching the supplied filters.
     * Returns a paginated result set with {@code showCount} per movie.
     */
    public PagedResponse<MovieSearchResult> searchMovies(
            String city, LocalDate date, String language, String genre, int page, int limit) {

        NativeQuery query = MovieSearchQuery.build(city, date, language, genre, page, limit);
        SearchHits<MovieDocument> hits = elasticsearchOperations.search(query, MovieDocument.class);

        List<MovieSearchResult> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(movieMapper::toSearchResult)
                .collect(Collectors.toList());

        long total     = hits.getTotalHits();
        int  totalPages = (limit > 0) ? (int) Math.ceil((double) total / limit) : 0;

        return PagedResponse.<MovieSearchResult>builder()
                .success(true)
                .content(results)
                .page(page)
                .size(limit)
                .totalElements(total)
                .totalPages(totalPages)
                .last(page >= totalPages - 1)
                .build();
    }

    // ── Get by ID (Redis cache → PostgreSQL fallback) ─────────────────────────

    /**
     * Returns the movie for the given ID.
     * Checks Redis first (TTL 1 hour); on a miss, loads from PostgreSQL and populates the cache.
     */
    @Transactional(readOnly = true)
    public MovieResponse getById(UUID id) {
        String cacheKey = CACHE_PREFIX + id;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, MovieResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialise cached movie {}, fetching from DB", id, e);
            }
        }

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));

        MovieResponse response = movieMapper.toResponse(movie);
        cacheMovie(cacheKey, response);
        return response;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Persists a new movie to PostgreSQL and publishes a {@link MovieCreatedEvent} so the
     * {@link com.booking.movie.infrastructure.elasticsearch.sync.MovieSyncScheduler} can
     * index it to Elasticsearch after the transaction commits.
     */
    @Transactional
    public MovieResponse createMovie(CreateMovieRequest req) {
        Movie movie = Movie.builder()
                .title(req.getTitle())
                .language(req.getLanguage())
                .genre(req.getGenre())
                .durationMins(req.getDurationMins())
                .rating(req.getRating())
                .releaseDate(req.getReleaseDate())
                .posterUrl(req.getPosterUrl())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : MovieStatus.UPCOMING)
                .build();

        movie = movieRepository.save(movie);
        log.info("Movie created: id={} title={}", movie.getId(), movie.getTitle());

        eventPublisher.publishEvent(new MovieCreatedEvent(movie));
        return movieMapper.toResponse(movie);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Applies patch-style updates to an existing movie.
     * Evicts the Redis cache entry and refreshes the Elasticsearch document on every write.
     */
    @Transactional
    public MovieResponse updateMovie(UUID id, UpdateMovieRequest req) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));

        if (req.getTitle()       != null) movie.setTitle(req.getTitle());
        if (req.getLanguage()    != null) movie.setLanguage(req.getLanguage());
        if (req.getGenre()       != null) movie.setGenre(req.getGenre());
        if (req.getDurationMins()!= null) movie.setDurationMins(req.getDurationMins());
        if (req.getRating()      != null) movie.setRating(req.getRating());
        if (req.getReleaseDate() != null) movie.setReleaseDate(req.getReleaseDate());
        if (req.getPosterUrl()   != null) movie.setPosterUrl(req.getPosterUrl());
        if (req.getDescription() != null) movie.setDescription(req.getDescription());
        if (req.getStatus()      != null) movie.setStatus(req.getStatus());

        movie = movieRepository.save(movie);
        log.info("Movie updated: id={}", id);

        // Evict stale cache entry
        redisTemplate.delete(CACHE_PREFIX + id);

        // Refresh ES document — preserve existing cities/showCount by fetching from ES first
        esRepository.findById(id.toString()).ifPresentOrElse(existing -> {
            MovieDocument updated = movieMapper.toDocument(movie);
            updated.setCities(existing.getCities());
            updated.setShowCount(existing.getShowCount());
            esRepository.save(updated);
        }, () -> esRepository.save(movieMapper.toDocument(movie)));

        return movieMapper.toResponse(movie);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void cacheMovie(String key, MovieResponse response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response),
                    Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache movie under key {}", key, e);
        }
    }
}
