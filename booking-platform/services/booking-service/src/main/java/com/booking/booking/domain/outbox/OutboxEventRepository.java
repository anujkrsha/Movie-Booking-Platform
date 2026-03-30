package com.booking.booking.domain.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link OutboxEvent} records.
 *
 * <p>All writes from the relay are done via targeted JPQL updates (no full entity load)
 * to keep the hot-path lean.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetches the next batch of events eligible for relay.
     *
     * <p>Includes PENDING records and FAILED records whose retry count is below the cap,
     * ordered oldest-first for fair processing. The {@code LIMIT} is applied in Java
     * via Spring Data's {@code Pageable} / first-N slice to avoid a native-query
     * portability issue; see {@link com.booking.booking.infrastructure.outbox.OutboxRelay}.
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = 'PENDING'
               OR (e.status = 'FAILED' AND e.retryCount < :maxRetries)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingBatch(@Param("maxRetries") int maxRetries,
                                       org.springframework.data.domain.Pageable pageable);

    /**
     * Marks a single event as PROCESSED and records the timestamp.
     * Uses a targeted UPDATE to avoid a round-trip SELECT + dirty-check.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = 'PROCESSED',
                e.processedAt = CURRENT_TIMESTAMP
            WHERE e.id = :id
            """)
    void markProcessed(@Param("id") UUID id);

    /**
     * Increments the retry counter for a failed publish attempt.
     * If {@code retryCount} reaches {@code maxRetries}, the next relay cycle will
     * mark the event FAILED via {@link #markFailed(UUID)}.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.retryCount = e.retryCount + 1
            WHERE e.id = :id
            """)
    void incrementRetryCount(@Param("id") UUID id);

    /**
     * Permanently marks an event as FAILED after retries are exhausted.
     * These records require manual intervention or a dedicated dead-letter processor.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = 'FAILED',
                e.processedAt = CURRENT_TIMESTAMP
            WHERE e.id = :id
            """)
    void markFailed(@Param("id") UUID id);
}
