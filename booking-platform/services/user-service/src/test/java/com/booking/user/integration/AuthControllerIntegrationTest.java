package com.booking.user.integration;

import com.booking.user.application.dto.LoginRequest;
import com.booking.user.application.dto.RegisterRequest;
import com.booking.user.support.IntegrationTestBase;
import com.booking.user.support.UserFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@code /v1/auth} and {@code /v1/users/me} endpoints.
 *
 * <p>Uses a real Spring Boot context backed by Testcontainers PostgreSQL and Redis.
 * Each test that modifies state is annotated with {@code @Sql} to truncate the
 * {@code users} table afterwards, keeping tests independent.
 */
@DisplayName("AuthController — integration")
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper     objectMapper;

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /v1/auth/register
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /v1/auth/register")
    class Register {

        @Test
        @DisplayName("valid payload → 201 Created with token pair")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void happyPath() {
            RegisterRequest req = UserFixtures.registerRequest();

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/register", req, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Map<?, ?> data = extractData(response.getBody());
            assertThat(data).containsKeys("access_token", "refresh_token", "token_type", "expires_in");
            assertThat(data.get("token_type")).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("duplicate email → 409 Conflict")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void duplicateEmail_returns409() {
            RegisterRequest req = UserFixtures.registerRequest();
            restTemplate.postForEntity("/v1/auth/register", req, Map.class); // seed

            ResponseEntity<Map> second = restTemplate.postForEntity(
                    "/v1/auth/register", req, Map.class);

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("duplicate phone but different email → 409 Conflict")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void duplicatePhone_returns409() {
            restTemplate.postForEntity("/v1/auth/register",
                    UserFixtures.registerRequest(), Map.class);

            RegisterRequest diff = UserFixtures.registerRequest(
                    "other@example.com", UserFixtures.DEFAULT_PHONE, "AnotherPass1!");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/register", diff, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("missing required field → 422 Unprocessable Entity")
        void missingField_returns422() {
            RegisterRequest bad = new RegisterRequest();
            bad.setEmail(""); // invalid — blank

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/register", bad, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /v1/auth/login
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /v1/auth/login")
    class Login {

        @Test
        @DisplayName("correct credentials → 200 OK with token pair")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void happyPath() {
            // Register first
            restTemplate.postForEntity("/v1/auth/register",
                    UserFixtures.registerRequest(), Map.class);

            ResponseEntity<Map> login = restTemplate.postForEntity(
                    "/v1/auth/login", UserFixtures.loginRequest(), Map.class);

            assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> data = extractData(login.getBody());
            assertThat(data).containsKey("access_token");
        }

        @Test
        @DisplayName("wrong password → 401 Unauthorized")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void wrongPassword_returns401() {
            restTemplate.postForEntity("/v1/auth/register",
                    UserFixtures.registerRequest(), Map.class);

            LoginRequest bad = UserFixtures.loginRequest(
                    UserFixtures.DEFAULT_EMAIL, "WrongPassword!");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/login", bad, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("non-existent email → 401 Unauthorized")
        void unknownEmail_returns401() {
            LoginRequest req = UserFixtures.loginRequest("nobody@example.com", "pass");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/login", req, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /v1/users/me
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /v1/users/me")
    class GetMe {

        @Test
        @DisplayName("valid Bearer token → 200 OK with profile")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void happyPath() {
            String accessToken = registerAndLogin();

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me",
                    HttpMethod.GET,
                    bearerRequest(accessToken),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> data = extractData(response.getBody());
            assertThat(data.get("email")).isEqualTo(UserFixtures.DEFAULT_EMAIL);
        }

        @Test
        @DisplayName("no Authorization header → 401 Unauthorized")
        void noToken_returns401() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("malformed token → 401 Unauthorized")
        void malformedToken_returns401() {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me",
                    HttpMethod.GET,
                    bearerRequest("not.a.valid.jwt"),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("blacklisted (logged-out) token → 401 Unauthorized")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void blacklistedToken_returns401() {
            String accessToken = registerAndLogin();

            // Logout — blacklists the token
            Map<String, String> logoutBody = Map.of("accessToken", accessToken);
            restTemplate.postForEntity("/v1/auth/logout", logoutBody, Map.class);

            // Attempt to use the blacklisted token
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/v1/users/me",
                    HttpMethod.GET,
                    bearerRequest(accessToken),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /v1/auth/refresh
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("valid refresh token → 200 OK with new token pair")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void happyPath() {
            // Register and get initial token pair
            ResponseEntity<Map> reg = restTemplate.postForEntity(
                    "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
            String refreshToken = (String) extractData(reg.getBody()).get("refresh_token");

            Map<String, String> body = Map.of("refreshToken", refreshToken);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/refresh", body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> data = extractData(response.getBody());
            assertThat(data).containsKey("access_token");
            // New tokens should be different from the originals
            assertThat(data.get("refresh_token")).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("invalid refresh token → 401 Unauthorized")
        void invalidToken_returns401() {
            Map<String, String> body = Map.of("refreshToken", "bad.token.here");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/v1/auth/refresh", body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("refresh token replay (used twice) → second use returns 401")
        @Sql(scripts = "classpath:db/reset.sql",
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void refreshReplay_returns401OnSecondUse() {
            ResponseEntity<Map> reg = restTemplate.postForEntity(
                    "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
            String refreshToken = (String) extractData(reg.getBody()).get("refresh_token");

            Map<String, String> body = Map.of("refreshToken", refreshToken);
            restTemplate.postForEntity("/v1/auth/refresh", body, Map.class); // first use — OK

            ResponseEntity<Map> second = restTemplate.postForEntity(
                    "/v1/auth/refresh", body, Map.class); // replay

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Registers the default user and returns an access token. */
    private String registerAndLogin() {
        ResponseEntity<Map> reg = restTemplate.postForEntity(
                "/v1/auth/register", UserFixtures.registerRequest(), Map.class);
        return (String) extractData(reg.getBody()).get("access_token");
    }

    /** Builds an HttpEntity with an {@code Authorization: Bearer <token>} header. */
    private static HttpEntity<?> bearerRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    /**
     * Extracts the {@code data} map from the standard {@code ApiResponse} envelope:
     * {@code { "status": "SUCCESS", "message": "...", "data": { ... } }}.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractData(Map<?, ?> body) {
        assertThat(body).isNotNull().containsKey("data");
        return (Map<String, Object>) body.get("data");
    }
}
