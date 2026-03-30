package com.booking.booking.infrastructure.outbox;

import com.booking.booking.domain.outbox.OutboxEvent;
import com.booking.booking.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Transactional outbox relay — polls {@code booking_outbox} and publishes pending events
 * to Kafka, ensuring at-least-once delivery even if the JVM crashes mid-flight.
 *
 * <h3>Execution model</h3>
 * <ul>
 *   <li>Runs every {@code outbox.relay.poll-interval-ms} milliseconds (default 5 000)
 *       with {@code fixedDelay} semantics — the next execution starts after the previous
 *       one finishes, so there is no concurrent relay overlap.</li>
 *   <li>Processes up to {@code outbox.relay.batch-size} events per cycle (default 50).</li>
 *   <li>Each event is published to the topic stored in {@link OutboxEvent#getTopic()}
 *       using {@link OutboxEvent#getAggregateId()} as the Kafka message key, which
 *       guarantees partition-level ordering per booking.</li>
 * </ul>
 *
 * <h3>Retry and failure handling</h3>
 * <ul>
 *   <li>On a failed Kafka send the retry counter is incremented.</li>
 *   <li>After {@code MAX_RETRIES} failed attempts the event is moved to FAILED status
 *       and excluded from future relay cycles — manual or dead-letter intervention
 *       is required.</li>
 *   <li>Idempotent producer config ({@code enable.idempotence=true}) on the Kafka side
 *       prevents duplicates caused by producer retries.</li>
 * </ul>
 *
 * <h3>Transactional boundary</h3>
 * <p>The DB status update ({@code markProcessed} / {@code incrementRetryCount}) runs in
 * its own transaction <em>after</em> the Kafka future completes.  The relay loop itself
 * is non-transactional so a slow Kafka response does not hold a DB connection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Main relay loop — runs every 5 seconds with a fixed delay.
     *
     * <p>Override the interval via {@code outbox.relay.poll-interval-ms} in
     * {@code application.yml} (the property is referenced in the SpEL expression below).
     */
    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:5000}")
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findPendingBatch(
                MAX_RETRIES,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (batch.isEmpty()) {
            return;
        }

        log.debug("OutboxRelay: processing {} event(s)", batch.size());

        for (OutboxEvent event : batch) {
            publishEvent(event);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void publishEvent(OutboxEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    event.getTopic(),
                    event.getAggregateId().toString(),  // message key → same partition per booking
                    event.getPayload()
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    onPublishSuccess(event, result);
                } else {
                    onPublishFailure(event, ex);
                }
            });

        } catch (Exception ex) {
            // Synchronous failure (e.g. serialisation error) — treat same as async failure
            onPublishFailure(event, ex);
        }
    }

    @Transactional
    protected void onPublishSuccess(OutboxEvent event, SendResult<String, Object> result) {
        outboxEventRepository.markProcessed(event.getId());
        log.info("OutboxRelay: published event {} ({}) to {}[{}]@{}",
                event.getId(),
                event.getEventType(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    @Transactional
    protected void onPublishFailure(OutboxEvent event, Throwable ex) {
        int newCount = event.getRetryCount() + 1;

        if (newCount >= MAX_RETRIES) {
            outboxEventRepository.markFailed(event.getId());
            log.error("OutboxRelay: event {} ({}) permanently FAILED after {} attempt(s) — manual intervention required. Cause: {}",
                    event.getId(), event.getEventType(), newCount, ex.getMessage());
        } else {
            outboxEventRepository.incrementRetryCount(event.getId());
            log.warn("OutboxRelay: publish failed for event {} ({}) — attempt {}/{}. Cause: {}",
                    event.getId(), event.getEventType(), newCount, MAX_RETRIES, ex.getMessage());
        }
    }
}
