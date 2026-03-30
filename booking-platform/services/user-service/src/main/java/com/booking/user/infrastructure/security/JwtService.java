package com.booking.user.infrastructure.security;

import com.booking.common.security.JwtProperties;
import com.booking.common.security.JwtUtil;
import com.booking.user.application.dto.TokenResponse;
import com.booking.user.domain.entity.User;
import com.booking.user.domain.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages the full JWT token lifecycle for user-service:
 * token issuance, refresh-token rotation, access-token blacklisting.
 *
 * <h3>Redis key schema</h3>
 * <pre>
 *   REFRESH:{userId}          →  {refreshToken jti}     TTL: 7 days
 *   BLACKLIST:{accessToken jti} →  "1"                  TTL: remaining access-token lifetime
 * </pre>
 *
 * <h3>Refresh-token rotation</h3>
 * <p>On every successful refresh:
 * <ol>
 *   <li>The incoming refresh token's JTI is looked up in Redis.</li>
 *   <li>If found and matching, a new access + refresh pair is issued.</li>
 *   <li>The old Redis entry is deleted; the new refresh JTI is stored.</li>
 * </ol>
 * A stolen refresh token that has already been rotated will no longer match the
 * stored JTI, causing the next use to return 401.
 *
 * <h3>Access-token blacklisting</h3>
 * <p>On logout, the access token's JTI is stored in Redis with TTL equal to
 * the token's remaining validity period. The {@link TokenBlacklistFilter}
 * checks this key before passing the request to the main JWT filter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    static final String REFRESH_KEY_PREFIX   = "REFRESH:";
    static final String BLACKLIST_KEY_PREFIX = "BLACKLIST:";

    private final JwtUtil        jwtUtil;
    private final JwtProperties  jwtProperties;

    @Qualifier("redisTemplate")
    private final RedisTemplate<String, String> redisTemplate;

    // ── Token issuance ───────────────────────────────────────────────────────

    /**
     * Issues a new access token + refresh token pair for the given user
     * and stores the refresh token JTI in Redis.
     */
    public TokenResponse issueTokenPair(User user) {
        String accessToken  = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        storeRefreshToken(user.getId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .build();
    }

    // ── Refresh-token rotation ────────────────────────────────────────────────

    /**
     * Validates a refresh token, issues a new access token, and rotates the refresh token.
     *
     * @throws AuthException if the token is structurally invalid, expired, or not in Redis
     */
    public TokenResponse rotateTokens(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw AuthException.refreshTokenInvalid();
        }

        UUID   userId        = jwtUtil.extractUserId(refreshToken);
        String incomingJti   = jwtUtil.extractJti(refreshToken);
        String storedJti     = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);

        if (storedJti == null || !storedJti.equals(incomingJti)) {
            // Token was already rotated or never issued from this server — possible replay attack
            log.warn("Refresh token replay or invalid for userId={}", userId);
            // Invalidate the stored token as a precaution (delete existing refresh)
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
            throw AuthException.refreshTokenInvalid();
        }

        // Look up current user details from claims (no DB call — email is in the access token,
        // but refresh tokens only carry sub; we need email + role for the new access token)
        // Solution: load from DB since refresh is a low-frequency operation
        // (injected below via UserRepository — see the note in the constructor)
        String email = lookupEmailForUser(userId);
        String role  = lookupRoleForUser(userId);

        // Issue new pair
        String newAccessToken  = jwtUtil.generateAccessToken(userId, email, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // Rotate: delete old, store new
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        storeRefreshToken(userId, newRefreshToken);

        log.debug("Refresh token rotated for userId={}", userId);
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .build();
    }

    // ── Logout / token revocation ─────────────────────────────────────────────

    /**
     * Blacklists the given access token and deletes the associated refresh token from Redis.
     *
     * @param accessToken the access token to revoke
     */
    public void revokeTokens(String accessToken) {
        if (!jwtUtil.validateToken(accessToken)) {
            // Already expired or invalid — nothing to revoke
            return;
        }

        String jti    = jwtUtil.extractJti(accessToken);
        UUID   userId = jwtUtil.extractUserId(accessToken);
        Date   expiry = jwtUtil.extractExpiry(accessToken);

        long remainingMs = expiry.getTime() - System.currentTimeMillis();
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + jti,
                    "1",
                    remainingMs,
                    TimeUnit.MILLISECONDS
            );
            log.debug("Access token blacklisted: jti={} ttl={}ms", jti, remainingMs);
        }

        // Also invalidate the refresh token so the entire session is ended
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        log.debug("Refresh token deleted for userId={}", userId);
    }

    // ── Blacklist check ───────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the access token has been blacklisted (i.e. the user
     * logged out but the token has not yet expired naturally).
     */
    public boolean isBlacklisted(String accessToken) {
        try {
            String jti = jwtUtil.extractJti(accessToken);
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception ex) {
            log.warn("Could not check blacklist for token: {}", ex.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void storeRefreshToken(UUID userId, String refreshToken) {
        String jti = jwtUtil.extractJti(refreshToken);
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                jti,
                Duration.ofMillis(jwtProperties.getRefreshExpiration())
        );
    }

    // These two methods are package-private so JwtService can be tested without UserRepository.
    // In production they are overridden by UserServiceAwareJwtService (a subclass).
    // Alternatively, inject UserRepository directly — kept separate here to avoid a circular bean.

    String lookupEmailForUser(UUID userId) {
        throw new UnsupportedOperationException(
                "Override this method or inject UserRepository — see UserServiceAwareJwtService");
    }

    String lookupRoleForUser(UUID userId) {
        throw new UnsupportedOperationException(
                "Override this method or inject UserRepository — see UserServiceAwareJwtService");
    }
}
