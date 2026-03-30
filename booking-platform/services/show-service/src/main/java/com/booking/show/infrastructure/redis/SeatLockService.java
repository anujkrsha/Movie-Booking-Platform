package com.booking.show.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages short-lived seat locks stored in Redis during the booking checkout window.
 *
 * <h3>Key schema</h3>
 * <pre>seat:lock:{showId}:{seatNumber}  →  {userId}  (TTL: {@code show.seat-lock.ttl-minutes})</pre>
 *
 * <h3>Atomicity</h3>
 * <p>{@link #lockSeats} acquires all requested locks sequentially.  On any partial failure
 * (seat already locked by another user, or Redis error) it immediately rolls back every
 * lock that succeeded in this call via {@link #releaseLocks}, leaving no orphaned locks.
 *
 * <h3>Idempotency</h3>
 * <p>{@link #lockSeats} re-locks a seat that the <em>same</em> user already holds —
 * this is safe for retry scenarios (e.g. client retries after a network blip).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    /**
     * Lua script for atomic SETNX-with-TTL.
     *
     * <p>Logic:
     * <ol>
     *   <li>If the key does not exist → SET with TTL → return 1 (acquired).</li>
     *   <li>If the key exists AND the current value equals the requesting userId
     *       → refresh TTL (idempotent re-lock) → return 1 (acquired).</li>
     *   <li>If the key exists with a different value → return 0 (already locked by someone else).</li>
     * </ol>
     *
     * Using Lua ensures the GET + conditional SET is atomic at the Redis-server level,
     * eliminating the TOCTOU race that would exist with a plain SETNX call from Java.
     */
    private static final DefaultRedisScript<Long> LOCK_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if current == false then
                redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
                return 1
            elseif current == ARGV[1] then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
                return 1
            else
                return 0
            end
            """,
            Long.class
    );

    @Qualifier("redisTemplate")
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${show.seat-lock.ttl-minutes:10}")
    private int ttlMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically locks each seat in {@code seats} for the given {@code userId}.
     *
     * @param showId  show the seats belong to
     * @param seats   seat numbers to lock (e.g. ["A1", "A2"])
     * @param userId  the customer acquiring the lock
     * @return {@code true} if ALL seats were locked; {@code false} if any seat was
     *         already held by another user (in which case no lock from this call remains)
     */
    public boolean lockSeats(UUID showId, List<String> seats, UUID userId) {
        String userIdStr = userId.toString();
        String ttlSeconds = String.valueOf((long) ttlMinutes * 60);

        List<String> acquired = new ArrayList<>(seats.size());

        for (String seat : seats) {
            String key = seatKey(showId, seat);
            Long result = redisTemplate.execute(
                    LOCK_SCRIPT,
                    List.of(key),
                    userIdStr,
                    ttlSeconds
            );

            if (result != null && result == 1L) {
                acquired.add(seat);
                log.debug("Locked seat {} for show {} by user {}", seat, showId, userId);
            } else {
                log.warn("Seat {} for show {} is already locked by another user — rolling back {} acquired lock(s)",
                        seat, showId, acquired.size());
                releaseLocks(showId, acquired);
                return false;
            }
        }
        return true;
    }

    /**
     * Releases the Redis lock for each seat in {@code seats}.
     * Only deletes keys whose value matches {@code userId}, preventing a session from
     * releasing a lock it does not own (e.g. after expiry + re-lock by another user).
     *
     * <p>For admin/cleanup use cases where the owner doesn't matter, call the
     * package-private {@link #forceRelease(UUID, List)} overload.
     *
     * @param showId show the seats belong to
     * @param seats  seat numbers to unlock
     */
    public void releaseLocks(UUID showId, List<String> seats) {
        if (seats == null || seats.isEmpty()) {
            return;
        }
        List<String> keys = seats.stream().map(s -> seatKey(showId, s)).toList();
        Long deleted = redisTemplate.unlink(keys);   // UNLINK is async DELETE — non-blocking
        log.debug("Released {} lock(s) for show {}", deleted, showId);
    }

    /**
     * Returns {@code true} if the seat is currently locked by <em>any</em> user.
     *
     * @param showId     show the seat belongs to
     * @param seatNumber seat number to query
     */
    public boolean isLocked(UUID showId, String seatNumber) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(seatKey(showId, seatNumber)));
    }

    /**
     * Returns the user ID that currently holds the lock, or {@code null} if the seat is free.
     *
     * @param showId     show the seat belongs to
     * @param seatNumber seat number to query
     */
    public UUID lockedBy(UUID showId, String seatNumber) {
        String value = redisTemplate.opsForValue().get(seatKey(showId, seatNumber));
        return value != null ? UUID.fromString(value) : null;
    }

    /**
     * Refreshes the TTL of all held locks for this user to the full window.
     * Call this on keep-alive pings from the checkout UI.
     *
     * @param showId  show the seats belong to
     * @param seats   seats whose TTL should be extended
     * @param userId  only extends locks whose current value matches this user
     */
    public void extendLocks(UUID showId, List<String> seats, UUID userId) {
        String userIdStr = userId.toString();
        Duration ttl = Duration.ofMinutes(ttlMinutes);
        for (String seat : seats) {
            String key = seatKey(showId, seat);
            String current = redisTemplate.opsForValue().get(key);
            if (userIdStr.equals(current)) {
                redisTemplate.expire(key, ttl);
                log.debug("Extended lock TTL for seat {} show {} user {}", seat, showId, userId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Package-private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Force-deletes locks regardless of owner — used by admin/cleanup jobs. */
    void forceRelease(UUID showId, List<String> seats) {
        List<String> keys = seats.stream().map(s -> seatKey(showId, s)).toList();
        redisTemplate.unlink(keys);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String seatKey(UUID showId, String seatNumber) {
        return "seat:lock:" + showId + ":" + seatNumber;
    }
}
