package com.booking.user.integration;

import com.booking.user.support.IntegrationTestBase;
import com.booking.user.support.UserFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

/**
 * Security-focused integration tests.
 *
 * <p>Covers:
 * <ul>
 *   <li>Unauthenticated access to protected endpoints → 401</li>
 *   <li>Public endpoints accessible without a token → 200/201</li>
 *   <li>JWT blacklist enforcement — token rejected after logout</li>
 *   <li>Token tampering → 401</li>
 *   <li>Refresh token rotation — replayed token rejected</li>
 * </ul>
 *
 * <p>Role-based access tests (CUSTOMER vs ADMIN) are omitted here because the
 * user-service itself does not expose any ADMIN-only endpoints; that concern belongs
 * to downstream services (theatre-service, show-service) which share the public key.
 */
@DisplayName("Security — integration")
class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate restTemplate;

    // ═══════════════════════════════════════════════════════════════════════════
    // Public endpoints — no token required
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("POST /v1/auth/register — accessible without token")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void register_publiclyAccessible() {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/register", UserFixtures.registerRequest(), Map.class);

            assertThat2xx(response);
        }

        @Test
        @DisplayName("POST /v1/auth/login — accessible without token")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void login_publiclyAccessible() {
            restTemplate.postForEntity("/v1/auth/register",
                    UserFixtures.registerRequest(), Map.class);

            ResponseEntity<Map> login = restTemplate.postForEntity(
                    "/v1/auth/login", UserFixtures.loginRequest(), Map.class);

            assertThat2xx(login);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Protected endpoint — GET /v1/users/me
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Protected endpoint /v1/users/me")
    class ProtectedEndpoint {

        @Test
        @DisplayName("no Authorization header → 401")
        void noToken_returns401() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), Map.class);

            assertStatusIs(response, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Authorization header missing 'Bearer ' prefix → 401")
        void missingBearerPrefix_returns401() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    headerRequest("Token some.token.here"), Map.class);

            assertStatusIs(response, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("tampered token (signature modified) → 401")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void tamperedToken_returns401() {
            String token = registerAndGetToken();
            String tampered = token + "x";

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    bearerRequest(tampered), Map.class);

            assertStatusIs(response, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("completely invalid token string → 401")
        void garbageToken_returns401() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    bearerRequest("not.a.jwt"), Map.class);

            assertStatusIs(response, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("valid token → 200 OK")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void validToken_returns200() {
            String token = registerAndGetToken();

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    bearerRequest(token), Map.class);

            assertStatusIs(response, HttpStatus.OK);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JWT blacklist — token rejected after logout
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("JWT blacklist after logout")
    class BlacklistAfterLogout {

        @Test
        @DisplayName("access token rejected immediately after logout → 401")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void tokenRejectedAfterLogout() {
            String accessToken = registerAndGetToken();

            // Confirm token is valid pre-logout
            ResponseEntity<Map> pre = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET, bearerRequest(accessToken), Map.class);
            assertStatusIs(pre, HttpStatus.OK);

            // Logout
            Map<String, String> logoutBody = Map.of("accessToken", accessToken);
            restTemplate.postForEntity("/v1/auth/logout", logoutBody, Map.class);

            // Same token must now be rejected
            ResponseEntity<Map> post = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET, bearerRequest(accessToken), Map.class);
            assertStatusIs(post, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("new token obtained after logout still works")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void newTokenAfterLogoutWorks() {
            String firstToken = registerAndGetToken();

            // Logout with the first token
            Map<String, String> logoutBody = Map.of("accessToken", firstToken);
            restTemplate.postForEntity("/v1/auth/logout", logoutBody, Map.class);

            // Re-login to get a fresh token
            ResponseEntity<Map> login = restTemplate.postForEntity(
                    "/v1/auth/login", UserFixtures.loginRequest(), Map.class);
            String newToken = (String) extractData(login.getBody()).get("access_token");

            // New token must be accepted
            ResponseEntity<Map> me = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET, bearerRequest(newToken), Map.class);
            assertStatusIs(me, HttpStatus.OK);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Refresh token rotation security
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Refresh token rotation")
    class RefreshRotation {

        @Test
        @DisplayName("refresh token works exactly once — replay rejected")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void refreshTokenSingleUse() {
            ResponseEntity<Map> reg = restTemplate.postForEntity(
                    "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
            String refreshToken = (String) extractData(reg.getBody()).get("refresh_token");

            Map<String, String> body = Map.of("refreshToken", refreshToken);

            // First rotation — must succeed
            ResponseEntity<Map> first = restTemplate.postForEntity(
                    "/v1/auth/refresh", body, Map.class);
            assertStatusIs(first, HttpStatus.OK);

            // Replay the original refresh token — must be rejected
            ResponseEntity<Map> replay = restTemplate.postForEntity(
                    "/v1/auth/refresh", body, Map.class);
            assertStatusIs(replay, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("new refresh token from rotation is accepted")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void rotatedRefreshTokenAccepted() {
            ResponseEntity<Map> reg = restTemplate.postForEntity(
                    "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
            String originalRefresh = (String) extractData(reg.getBody()).get("refresh_token");

            // Rotate once
            ResponseEntity<Map> rotated = restTemplate.postForEntity(
                    "/v1/auth/refresh", Map.of("refreshToken", originalRefresh), Map.class);
            String newRefresh = (String) extractData(rotated.getBody()).get("refresh_token");

            // Use the newly issued refresh token — must succeed
            ResponseEntity<Map> second = restTemplate.postForEntity(
                    "/v1/auth/refresh", Map.of("refreshToken", newRefresh), Map.class);
            assertStatusIs(second, HttpStatus.OK);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Response structure (API envelope)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error response format")
    class ErrorResponseFormat {

        @Test
        @DisplayName("401 response includes standard error body (no stack trace)")
        void unauthorisedResponseBody() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me", HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), Map.class);

            assertStatusIs(response, HttpStatus.UNAUTHORIZED);
            // Body must exist and must NOT contain a stack trace key
            Map<?, ?> body = response.getBody();
            org.assertj.core.api.Assertions.assertThat(body).isNotNull();
            org.assertj.core.api.Assertions.assertThat(body).doesNotContainKey("trace");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String registerAndGetToken() {
        ResponseEntity<Map> reg = restTemplate.postForEntity(
                "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
        return (String) extractData(reg.getBody()).get("access_token");
    }

    private static HttpEntity<?> bearerRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return new HttpEntity<>(headers);
    }

    private static HttpEntity<?> headerRequest(String headerValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, headerValue);
        return new HttpEntity<>(headers);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractData(Map<?, ?> body) {
        org.assertj.core.api.Assertions.assertThat(body).isNotNull().containsKey("data");
        return (Map<String, Object>) body.get("data");
    }

    private static void assertStatusIs(ResponseEntity<?> response, HttpStatus expected) {
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode()).isEqualTo(expected);
    }

    private static void assertThat2xx(ResponseEntity<?> response) {
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
