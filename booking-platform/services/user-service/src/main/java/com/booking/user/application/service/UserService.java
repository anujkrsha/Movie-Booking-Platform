package com.booking.user.application.service;

import com.booking.common.exception.ConflictException;
import com.booking.user.application.dto.LoginRequest;
import com.booking.user.application.dto.LogoutRequest;
import com.booking.user.application.dto.RefreshRequest;
import com.booking.user.application.dto.RegisterRequest;
import com.booking.user.application.dto.TokenResponse;
import com.booking.user.application.dto.UserProfileResponse;
import com.booking.user.domain.entity.User;
import com.booking.user.domain.entity.UserRole;
import com.booking.user.domain.exception.AuthException;
import com.booking.user.domain.repository.UserRepository;
import com.booking.user.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core authentication and profile service for user-service.
 *
 * <h3>Security decisions</h3>
 * <ul>
 *   <li>BCrypt with strength 12 — ~300 ms per hash on a modern CPU, acceptable for login
 *       but impractical for offline brute-force attacks.</li>
 *   <li>Constant-time response for invalid email — {@code passwordEncoder.matches()} is
 *       always called even when the user is not found, preventing timing-based user enumeration.</li>
 *   <li>Refresh tokens are rotated on every use — a stolen, replayed refresh token is
 *       detected on the second use (Redis key will no longer match).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER =
            new BCryptPasswordEncoder(12);

    private final UserRepository userRepository;
    private final JwtService     jwtService;

    // ── Register ─────────────────────────────────────────────────────────────

    /**
     * Creates a new CUSTOMER account, then issues an immediate JWT pair so the user
     * does not need a separate login step after registration.
     *
     * @throws ConflictException if the email or phone is already registered
     */
    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email address is already registered");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new ConflictException("Phone number is already registered");
        }

        User user = User.builder()
                .email(req.getEmail().toLowerCase().strip())
                .phone(req.getPhone().strip())
                .passwordHash(PASSWORD_ENCODER.encode(req.getPassword()))
                .role(UserRole.CUSTOMER)
                .city(req.getCity())
                .build();

        userRepository.save(user);
        log.info("New user registered: id={} email={}", user.getId(), user.getEmail());

        return jwtService.issueTokenPair(user);
    }

    // ── Login ────────────────────────────────────────────────────────────────

    /**
     * Validates credentials and issues a fresh JWT pair.
     *
     * <p>Timing-safe: even when no user is found for the given email, the password
     * encoder's {@code matches()} call is still performed against a dummy hash so the
     * response time is indistinguishable from a real credential mismatch.
     *
     * @throws AuthException (401) on invalid credentials
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        // Normalise email so "User@Example.COM" and "user@example.com" resolve to the same account
        String normalisedEmail = req.getEmail().toLowerCase().strip();

        User user = userRepository.findByEmail(normalisedEmail)
                .orElse(null);

        // Always run the hash comparison — prevents timing-based email enumeration
        String hashToCheck = (user != null)
                ? user.getPasswordHash()
                : "$2a$12$invalidhashusedfortimingprotection000000000000000000000";

        if (user == null || !PASSWORD_ENCODER.matches(req.getPassword(), hashToCheck)) {
            throw AuthException.invalidCredentials();
        }

        log.info("User logged in: id={} email={}", user.getId(), user.getEmail());
        return jwtService.issueTokenPair(user);
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    /**
     * Validates the provided refresh token against the Redis store, then issues a new
     * access token and rotates the refresh token (old one is deleted, new one stored).
     *
     * @throws AuthException (401) if the refresh token is invalid, expired, or already used
     */
    public TokenResponse refresh(RefreshRequest req) {
        return jwtService.rotateTokens(req.getRefreshToken());
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    /**
     * Blacklists the provided access token in Redis and deletes the corresponding
     * refresh token, effectively ending all active sessions for this token pair.
     */
    public void logout(LogoutRequest req) {
        jwtService.revokeTokens(req.getAccessToken());
        log.info("User logged out — access token blacklisted");
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param userId UUID from the JWT {@code sub} claim
     * @throws com.booking.common.exception.ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.booking.common.exception.ResourceNotFoundException(
                        "User", userId));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .city(user.getCity())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
