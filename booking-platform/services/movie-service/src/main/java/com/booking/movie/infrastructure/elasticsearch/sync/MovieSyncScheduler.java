package com.booking.movie.infrastructure.elasticsearch.sync;

import com.booking.movie.application.mapper.MovieMapper;
import com.booking.movie.domain.event.MovieCreatedEvent;
import com.booking.movie.domain.repository.MovieRepository;
import com.booking.movie.infrastructure.elasticsearch.document.MovieDocument;
import com.booking.movie.infrastructure.elasticsearch.repository.MovieElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles two Elasticsearch synchronisation concerns:
 *
 * <ol>
 *   <li><b>Real-time:</b> listens for {@link MovieCreatedEvent} (via
 *       {@code @TransactionalEventListener}) and indexes the new document immediately after
 *       the creating transaction commits.</li>
 *   <li><b>Daily bulk sync:</b> a {@code @Scheduled} job that re-indexes every active movie
 *       from PostgreSQL to Elasticsearch once per day, correcting any drift that may have
 *       occurred (e.g. failed event deliveries, manual DB patches).  The {@code showCount}
 *       field is intended to be updated here once show-service integration is available.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieSyncScheduler {

    private final MovieRepository              movieRepository;
    private final MovieElasticsearchRepository esRepository;
    private final MovieMapper                  movieMapper;

    // ── Real-time indexing on create ─────────────────────────────────────────

    /**
     * Indexes a newly created movie to Elasticsearch after its DB transaction commits.
     *
     * <p>Using {@link TransactionPhase#AFTER_COMMIT} guarantees that the ES document is
     * written only when the row is durably persisted in PostgreSQL.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieCreated(MovieCreatedEvent event) {
        MovieDocument doc = movieMapper.toDocument(event.getMovie());
        esRepository.save(doc);
        log.info("Indexed new movie to ES: id={} title={}",
                event.getMovie().getId(), event.getMovie().getTitle());
    }

    // ── Daily bulk sync ──────────────────────────────────────────────────────

    /**
     * Full re-index of all active (non-archived) movies from PostgreSQL to Elasticsearch.
     *
     * <p>Runs every day at 02:00 UTC. The cron expression can be overridden via
     * {@code movie.sync.cron} in {@code application.yml}.
     *
     * <p><b>show-service integration note:</b> when show-service exposes an endpoint for
     * show counts per movie, this method should call it (or consume a Kafka summary topic)
     * to populate {@code showCount} and {@code cities} on each document.
     */
    @Scheduled(cron = "${movie.sync.cron:0 0 2 * * *}")
    public void dailySync() {
        log.info("Starting daily movie ES sync");

        List<MovieDocument> docs = movieRepository.findAllActive()
                .stream()
                .map(movie -> {
                    MovieDocument doc = movieMapper.toDocument(movie);
                    // Preserve existing cities/showCount from the current ES document
                    esRepository.findById(movie.getId().toString()).ifPresent(existing -> {
                        doc.setCities(existing.getCities());
                        doc.setShowCount(existing.getShowCount());
                    });
                    return doc;
                })
                .collect(Collectors.toList());

        esRepository.saveAll(docs);
        log.info("Daily ES sync complete — {} movies indexed", docs.size());
    }
}
