package com.booking.user.unit.service;

import com.booking.common.exception.ConflictException;
import com.booking.user.application.dto.LoginRequest;
import com.booking.user.application.dto.LogoutRequest;
import com.booking.user.application.dto.RefreshRequest;
import com.booking.user.application.dto.RegisterRequest;
import com.booking.user.application.dto.TokenResponse;
import com.booking.user.application.service.UserService;
import com.booking.user.domain.entity.User;
import com.booking.user.domain.entity.UserRole;
import com.booking.user.domain.exception.AuthException;
import com.booking.user.domain.repository.UserRepository;
import com.booking.user.infrastructure.security.JwtService;
import com.booking.user.support.UserFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link UserService}.
 *
 * <p>No Spring context — {@link UserRepository} and {@link JwtService} are Mockito mocks.
 * Password hashing uses BCrypt strength 4 to keep test runtime well under a second.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    /** Strength 4 — matches what {@link UserFixtures} uses; irrelevant for security here. */
    private static final BCryptPasswordEncoder FAST_ENCODER = new BCryptPasswordEncoder(4);

    @Mock private UserRepository userRepository;
    @Mock private JwtService     jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtService);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // register()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest request;
        private TokenResponse   expectedTokens;

        @BeforeEach
        void setUpRegister() {
            request        = UserFixtures.registerRequest();
            expectedTokens = TokenResponse.builder()
                    .accessToken("access.token.here")
                    .refreshToken("refresh.token.here")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();

            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByPhone(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
            given(jwtService.issueTokenPair(any(User.class))).willReturn(expectedTokens);
        }

        @Test
        @DisplayName("happy path — saves user with CUSTOMER role and returns token pair")
        void happyPath() {
            TokenResponse result = userService.register(request);

            assertThat(result).isSameAs(expectedTokens);

            // Verify the persisted user has correct fields
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo(request.getEmail().toLowerCase().strip());
            assertThat(saved.getPhone()).isEqualTo(request.getPhone().strip());
            assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(saved.getCity()).isEqualTo(request.getCity());
            // Password must be a BCrypt hash, not plain text
            assertThat(saved.getPasswordHash()).startsWith("$2a$");
            assertThat(saved.getPasswordHash()).isNotEqualTo(request.getPassword());
        }

        @Test
        @DisplayName("email normalisation — mixed-case input stored lowercase")
        void emailNormalisedToLowerCase() {
            request.setEmail("Alice@Example.COM");

            userService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("duplicate email → ConflictException, no save, no token issued")
        void duplicateEmail_throwsConflict() {
            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email");

            then(userRepository).should(never()).save(any());
            then(jwtService).should(never()).issueTokenPair(any());
        }

        @Test
        @DisplayName("duplicate phone → ConflictException, no save, no token issued")
        void duplicatePhone_throwsConflict() {
            given(userRepository.existsByPhone(request.getPhone())).willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Phone");

            then(userRepository).should(never()).save(any());
            then(jwtService).should(never()).issueTokenPair(any());
        }

        @Test
        @DisplayName("email check runs before phone check (short-circuit order)")
        void emailCheckedBeforePhone() {
            given(userRepository.existsByEmail(anyString())).willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(ConflictException.class);

            // Phone check must not be reached
            then(userRepository).should(never()).existsByPhone(anyString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // login()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        private User          storedUser;
        private LoginRequest  request;
        private TokenResponse expectedTokens;

        @BeforeEach
        void setUpLogin() {
            // Build a stored user whose hash we know (strength 4 for speed)
            storedUser = UserFixtures.savedUser();   // email=alice@example.com, pw=SecurePass123!
            // Re-hash with strength 4 so matches() is fast in tests
            injectPasswordHash(storedUser, FAST_ENCODER.encode(UserFixtures.DEFAULT_PASSWORD));

            request = UserFixtures.loginRequest();   // same email + password

            expectedTokens = TokenResponse.builder()
                    .accessToken("access.ok")
                    .refreshToken("refresh.ok")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();

            given(userRepository.findByEmail(UserFixtures.DEFAULT_EMAIL))
                    .willReturn(Optional.of(storedUser));
            given(jwtService.issueTokenPair(storedUser)).willReturn(expectedTokens);
        }

        @Test
        @DisplayName("correct credentials → issues token pair")
        void happyPath() {
            TokenResponse result = userService.login(request);
            assertThat(result).isSameAs(expectedTokens);
        }

        @Test
        @DisplayName("email is normalised before repository lookup")
        void emailNormalisedBeforeLookup() {
            request.setEmail("  Alice@Example.COM  ");

            // Repository stub is set for the normalised form — will resolve to storedUser
            given(userRepository.findByEmail("alice@example.com"))
                    .willReturn(Optional.of(storedUser));

            assertThatCode(() -> userService.login(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("wrong password → AuthException (401)")
        void wrongPassword_throwsAuthException() {
            request.setPassword("WrongPassword!");

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(AuthException.class);

            then(jwtService).should(never()).issueTokenPair(any());
        }

        @Test
        @DisplayName("non-existent email → AuthException (no token issued)")
        void nonExistentEmail_throwsAuthException() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(AuthException.class);

            then(jwtService).should(never()).issueTokenPair(any());
        }

        @Test
        @DisplayName("non-existent user — matches() is still called (timing safety)")
        void nonExistentUser_matchesStillCalled() {
            // We can't directly verify that matches() is called on the dummy hash (it's internal),
            // but we can assert that the method does not short-circuit with a NullPointerException
            // and that the response time path is exercised without error before throwing.
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // Must throw AuthException, not NullPointerException or anything else
            assertThatThrownBy(() -> userService.login(request))
                    .isExactlyInstanceOf(AuthException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // refresh()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";

        @Test
        @DisplayName("valid refresh token → delegates to JwtService and returns new pair")
        void happyPath() {
            TokenResponse newPair = TokenResponse.builder()
                    .accessToken("new.access")
                    .refreshToken("new.refresh")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();
            given(jwtService.rotateTokens(VALID_REFRESH_TOKEN)).willReturn(newPair);

            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken(VALID_REFRESH_TOKEN);

            TokenResponse result = userService.refresh(req);

            assertThat(result).isSameAs(newPair);
            then(jwtService).should().rotateTokens(VALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("invalid/expired refresh token → AuthException propagated from JwtService")
        void invalidToken_propagatesAuthException() {
            given(jwtService.rotateTokens(anyString()))
                    .willThrow(AuthException.tokenInvalid());

            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken("bad.token");

            assertThatThrownBy(() -> userService.refresh(req))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("already-rotated (revoked) refresh token → AuthException propagated")
        void revokedToken_propagatesAuthException() {
            given(jwtService.rotateTokens(anyString()))
                    .willThrow(AuthException.refreshTokenInvalid());

            RefreshRequest req = new RefreshRequest();
            req.setRefreshToken("rotated.token");

            assertThatThrownBy(() -> userService.refresh(req))
                    .isInstanceOf(AuthException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // logout()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("valid access token → delegates revocation to JwtService")
        void happyPath() {
            String accessToken = "valid.access.token";
            LogoutRequest req = new LogoutRequest();
            req.setAccessToken(accessToken);

            userService.logout(req);

            then(jwtService).should().revokeTokens(accessToken);
        }

        @Test
        @DisplayName("invalid access token → revokeTokens called (no-op path, no exception bubbled)")
        void invalidToken_noException() {
            // JwtService.revokeTokens is documented as a no-op for invalid tokens
            willDoNothing().given(jwtService).revokeTokens(anyString());

            LogoutRequest req = new LogoutRequest();
            req.setAccessToken("garbage.token");

            assertThatCode(() -> userService.logout(req)).doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getProfile()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("existing userId → returns mapped profile response")
        void happyPath() {
            User user = UserFixtures.savedUser();
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            var profile = userService.getProfile(user.getId());

            assertThat(profile.getId()).isEqualTo(user.getId());
            assertThat(profile.getEmail()).isEqualTo(user.getEmail());
            assertThat(profile.getPhone()).isEqualTo(user.getPhone());
            assertThat(profile.getRole()).isEqualTo(user.getRole());
            assertThat(profile.getCity()).isEqualTo(user.getCity());
        }

        @Test
        @DisplayName("unknown userId → ResourceNotFoundException")
        void unknownUser_throwsNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(unknownId))
                    .isInstanceOf(com.booking.common.exception.ResourceNotFoundException.class);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Injects a password hash into a User's passwordHash field via reflection,
     * bypassing the entity's builder/constructor restrictions.
     */
    private static void injectPasswordHash(User user, String hash) {
        try {
            var field = User.class.getDeclaredField("passwordHash");
            field.setAccessible(true);
            field.set(user, hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject password hash into User", e);
        }
    }
}
