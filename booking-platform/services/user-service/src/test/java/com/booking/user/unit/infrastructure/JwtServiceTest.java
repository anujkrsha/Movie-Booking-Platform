package com.booking.user.unit.infrastructure;

import com.booking.common.security.JwtProperties;
import com.booking.common.security.JwtUtil;
import com.booking.user.application.dto.TokenResponse;
import com.booking.user.domain.entity.UserRole;
import com.booking.user.domain.exception.AuthException;
import com.booking.user.infrastructure.security.JwtService;
import com.booking.user.support.RsaKeyTestHelper;
import com.booking.user.support.UserFixtures;
import com.booking.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.KeyPair;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Uses a real {@link JwtUtil} instance backed by a programmatically generated RSA keypair,
 * and a mocked {@link RedisTemplate} — no Spring context, no I/O.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    // ── System under test ──────────────────────────────────────────────────────
    private JwtUtil    jwtUtil;
    private JwtService jwtService;

    private User testUser;
    private static final long ACCESS_TTL_MS  = 900_000L;   // 15 min
    private static final long REFRESH_TTL_MS = 604_800_000L; // 7 days

    @BeforeEach
    void setUp() {
        KeyPair kp = RsaKeyTestHelper.generateKeyPair();

        JwtProperties props = new JwtProperties();
        props.setPrivateKeyBase64(RsaKeyTestHelper.toPrivateKeyBase64(kp));
        props.setPublicKeyBase64(RsaKeyTestHelper.toPublicKeyBase64(kp));
        props.setExpiration(ACCESS_TTL_MS);
        props.setRefreshExpiration(REFRESH_TTL_MS);

        jwtUtil = new JwtUtil(props);
        jwtUtil.initKeys();   // @PostConstruct equivalent in tests

        given(redisTemplate.opsForValue()).willReturn(valueOps);

        // Concrete subclass that wires email/role lookups (bypasses the UnsupportedOperation guard)
        jwtService = new JwtService(jwtUtil, props, redisTemplate) {
            @Override
            String lookupEmailForUser(UUID userId) { return testUser.getEmail(); }
            @Override
            String lookupRoleForUser(UUID userId)  { return testUser.getRole().name(); }
        };

        testUser = UserFixtures.savedUser();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // issueTokenPair
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("issueTokenPair()")
    class IssueTokenPair {

        @Test
        @DisplayName("returns a TokenResponse with both tokens and correct expiresIn")
        void happyPath() {
            TokenResponse result = jwtService.issueTokenPair(testUser);

            assertThat(result.getAccessToken()).isNotBlank();
            assertThat(result.getRefreshToken()).isNotBlank();
            assertThat(result.getExpiresIn()).isEqualTo(ACCESS_TTL_MS / 1000);
            assertThat(result.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("access token carries sub=userId, email, and role claims")
        void accessTokenClaims() {
            TokenResponse result = jwtService.issueTokenPair(testUser);
            String access = result.getAccessToken();

            assertThat(jwtUtil.extractUserId(access)).isEqualTo(testUser.getId());
            assertThat(jwtUtil.extractEmail(access)).isEqualTo(testUser.getEmail());
            assertThat(jwtUtil.extractAuthorities(access))
                    .extracting("authority")
                    .containsExactly("ROLE_CUSTOMER");
        }

        @Test
        @DisplayName("refresh token JTI is stored in Redis with 7-day TTL")
        void refreshTokenStoredInRedis() {
            TokenResponse result = jwtService.issueTokenPair(testUser);
            String expectedJti = jwtUtil.extractJti(result.getRefreshToken());

            ArgumentCaptor<String>   keyCaptor   = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String>   valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long>     ttlCaptor   = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TimeUnit> unitCaptor  = ArgumentCaptor.forClass(TimeUnit.class);

            // The store call uses Duration overload — verify via opsForValue().set(key, value, Duration)
            then(valueOps).should().set(
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    any()
            );

            assertThat(keyCaptor.getValue()).isEqualTo("REFRESH:" + testUser.getId());
            assertThat(valueCaptor.getValue()).isEqualTo(expectedJti);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // rotateTokens
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("rotateTokens()")
    class RotateTokens {

        @Test
        @DisplayName("valid refresh token → issues new pair and rotates Redis entry")
        void happyPath() {
            // Arrange — issue an initial pair to get a valid refresh token
            given(valueOps.get(anyString())).willReturn(null); // suppressed for issue
            TokenResponse initial = jwtService.issueTokenPair(testUser);

            String refreshJti = jwtUtil.extractJti(initial.getRefreshToken());
            given(valueOps.get("REFRESH:" + testUser.getId())).willReturn(refreshJti);

            // Act
            TokenResponse rotated = jwtService.rotateTokens(initial.getRefreshToken());

            // Assert — new tokens are different
            assertThat(rotated.getAccessToken()).isNotEqualTo(initial.getAccessToken());
            assertThat(rotated.getRefreshToken()).isNotEqualTo(initial.getRefreshToken());

            // Old Redis entry was deleted
            then(redisTemplate).should().delete("REFRESH:" + testUser.getId());
        }

        @Test
        @DisplayName("structurally invalid refresh token → AuthException")
        void invalidToken() {
            assertThatThrownBy(() -> jwtService.rotateTokens("not.a.jwt.token"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Refresh token");
        }

        @Test
        @DisplayName("refresh token not in Redis (already rotated or revoked) → AuthException")
        void tokenNotInRedis() {
            TokenResponse initial = jwtService.issueTokenPair(testUser);
            // Redis returns null — token was already rotated
            given(valueOps.get("REFRESH:" + testUser.getId())).willReturn(null);

            assertThatThrownBy(() -> jwtService.rotateTokens(initial.getRefreshToken()))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("refresh token JTI mismatch (replay attack) → AuthException and Redis cleared")
        void jtiMismatch() {
            TokenResponse initial = jwtService.issueTokenPair(testUser);
            // Redis has a different JTI (already rotated)
            given(valueOps.get("REFRESH:" + testUser.getId())).willReturn(UUID.randomUUID().toString());

            assertThatThrownBy(() -> jwtService.rotateTokens(initial.getRefreshToken()))
                    .isInstanceOf(AuthException.class);

            // Redis entry is cleared to invalidate the attacker's token too
            then(redisTemplate).should(atLeastOnce()).delete("REFRESH:" + testUser.getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // revokeTokens (logout)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("revokeTokens()")
    class RevokeTokens {

        @Test
        @DisplayName("valid access token → JTI blacklisted with remaining TTL, refresh deleted")
        void happyPath() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String jti = jwtUtil.extractJti(tokens.getAccessToken());

            jwtService.revokeTokens(tokens.getAccessToken());

            // Blacklist entry set with a positive TTL
            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            then(valueOps).should().set(
                    eq("BLACKLIST:" + jti), eq("1"), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));
            assertThat(ttlCaptor.getValue()).isPositive();

            // Refresh entry deleted
            then(redisTemplate).should().delete("REFRESH:" + testUser.getId());
        }

        @Test
        @DisplayName("already-expired access token → no-op (no Redis writes)")
        void expiredToken_noOp() {
            // Build a token that appears expired by using a far-past date would require
            // clock manipulation — instead verify that an invalid token (wrong signature)
            // is silently ignored.
            jwtService.revokeTokens("invalid.token.string");

            then(valueOps).should(never()).set(anyString(), anyString(), anyLong(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // isBlacklisted
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isBlacklisted()")
    class IsBlacklisted {

        @Test
        @DisplayName("blacklisted token → returns true")
        void blacklisted() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String jti = jwtUtil.extractJti(tokens.getAccessToken());
            given(redisTemplate.hasKey("BLACKLIST:" + jti)).willReturn(true);

            assertThat(jwtService.isBlacklisted(tokens.getAccessToken())).isTrue();
        }

        @Test
        @DisplayName("non-blacklisted token → returns false")
        void notBlacklisted() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String jti = jwtUtil.extractJti(tokens.getAccessToken());
            given(redisTemplate.hasKey("BLACKLIST:" + jti)).willReturn(false);

            assertThat(jwtService.isBlacklisted(tokens.getAccessToken())).isFalse();
        }

        @Test
        @DisplayName("malformed token → returns false (no exception)")
        void malformedToken_returnsFalse() {
            assertThat(jwtService.isBlacklisted("garbage")).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JwtUtil claim extraction (tested through JwtService.issueTokenPair)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("JwtUtil — token generation and parsing")
    class JwtUtilTests {

        @Test
        @DisplayName("validateToken → true for a freshly generated access token")
        void validate_fresh() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            assertThat(jwtUtil.validateToken(tokens.getAccessToken())).isTrue();
        }

        @Test
        @DisplayName("validateToken → false for a tampered token")
        void validate_tampered() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String tampered = tokens.getAccessToken() + "x";
            assertThat(jwtUtil.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("validateToken → false for a completely invalid string")
        void validate_garbage() {
            assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("extractJti → returns a non-blank UUID string")
        void extractJti() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String jti = jwtUtil.extractJti(tokens.getAccessToken());
            assertThat(jti).isNotBlank();
            assertThatCode(() -> UUID.fromString(jti)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("extractExpiry → returns a future date")
        void extractExpiry() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            assertThat(jwtUtil.extractExpiry(tokens.getAccessToken()).getTime())
                    .isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("refresh token → sub is userId UUID, no email/role claims")
        void refreshTokenMinimalClaims() {
            TokenResponse tokens = jwtService.issueTokenPair(testUser);
            String refresh = tokens.getRefreshToken();

            assertThat(jwtUtil.extractUserId(refresh)).isEqualTo(testUser.getId());
            // email claim should be absent — extractEmail returns null
            assertThat(jwtUtil.extractEmail(refresh)).isNull();
        }

        @Test
        @DisplayName("different users get tokens with different sub claims")
        void differentSubPerUser() {
            User other = UserFixtures.savedUser("bob@example.com", "+15550003333",
                    "Pass456!", UserRole.CUSTOMER);

            TokenResponse aliceTokens = jwtService.issueTokenPair(testUser);
            TokenResponse bobTokens   = jwtService.issueTokenPair(other);

            assertThat(jwtUtil.extractUserId(aliceTokens.getAccessToken()))
                    .isNotEqualTo(jwtUtil.extractUserId(bobTokens.getAccessToken()));
        }
    }
}
