package com.booking.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Stateless JWT utility using RS256 (RSA + SHA-256) via JJWT 0.12.x.
 *
 * <h3>Token payload</h3>
 * <pre>
 * {
 *   "sub":   "&lt;userId UUID&gt;",
 *   "email": "user@example.com",
 *   "role":  "CUSTOMER",
 *   "jti":   "&lt;random UUID — used for blacklisting on logout&gt;",
 *   "iat":   &lt;epoch seconds&gt;,
 *   "exp":   &lt;epoch seconds&gt;
 * }
 * </pre>
 *
 * <h3>Key configuration</h3>
 * <ul>
 *   <li><b>user-service</b>: configure both {@code jwt.private-key-base64} and
 *       {@code jwt.public-key-base64}.</li>
 *   <li><b>all other services</b>: configure only {@code jwt.public-key-base64}
 *       (token verification only).</li>
 * </ul>
 *
 * <p>Generate a 2048-bit keypair:
 * <pre>
 *   openssl genrsa -out rsa.pem 2048
 *   openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa.pem -nocrypt | base64 -w0
 *   openssl rsa -in rsa.pem -pubout -outform DER | base64 -w0
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    static final String CLAIM_EMAIL = "email";
    static final String CLAIM_ROLE  = "role";

    private final JwtProperties jwtProperties;

    private RSAPublicKey  publicKey;
    private RSAPrivateKey privateKey;  // null on services that only verify tokens

    @PostConstruct
    void initKeys() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            String pubBase64 = jwtProperties.getPublicKeyBase64();
            if (pubBase64 == null || pubBase64.isBlank()) {
                throw new IllegalStateException(
                        "jwt.public-key-base64 must be configured on every service");
            }
            byte[] pubBytes = Base64.getDecoder().decode(pubBase64.strip());
            publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            log.info("JwtUtil: RSA public key loaded");

            String privBase64 = jwtProperties.getPrivateKeyBase64();
            if (privBase64 != null && !privBase64.isBlank()) {
                byte[] privBytes = Base64.getDecoder().decode(privBase64.strip());
                privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                log.info("JwtUtil: RSA private key loaded — token issuance enabled");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise JWT RSA keys", e);
        }
    }

    // ── Token generation (requires private key) ──────────────────────────────

    /**
     * Generates an RS256-signed access token.
     * Claims: sub=userId, email, role, jti (for blacklisting), iat, exp.
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        requirePrivateKey();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generates an RS256-signed refresh token.
     * Minimal claims: sub=userId, jti, iat, exp. No role/email — those are in the access token.
     */
    public String generateRefreshToken(UUID userId) {
        requirePrivateKey();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration()))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    // ── Token parsing ────────────────────────────────────────────────────────

    /** Returns the user's UUID from the {@code sub} claim. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /** Returns the {@code email} claim. */
    public String extractEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    /**
     * Returns the email claim as the principal username.
     * Used by {@link JwtAuthFilter} for compatibility with Spring Security.
     */
    public String extractUsername(String token) {
        return extractEmail(token);
    }

    /** Returns the {@code role} claim wrapped in a Spring Security authority list. */
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Returns the {@code jti} claim — used to blacklist a token on logout. */
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    /** Returns the token's {@code exp} as a {@link Date}. */
    public Date extractExpiry(String token) {
        return parseClaims(token).getExpiration();
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the RS256 signature is valid and the token has not expired.
     *
     * <p>Does NOT check the Redis blacklist — that is handled by the service-specific
     * {@code TokenBlacklistFilter} registered before this filter in the chain.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void requirePrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException(
                    "jwt.private-key-base64 is not configured — this instance cannot issue tokens");
        }
    }
}
