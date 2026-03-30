package com.booking.user.support;

import com.booking.user.application.dto.LoginRequest;
import com.booking.user.application.dto.RegisterRequest;
import com.booking.user.domain.entity.User;
import com.booking.user.domain.entity.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

/**
 * Shared test data builders.
 *
 * <p>BCrypt strength is reduced to 4 in test fixtures — the security margin does not
 * matter for tests, and low strength keeps unit-test runtime under 50 ms per hash.
 */
public final class UserFixtures {

    public static final String DEFAULT_EMAIL    = "alice@example.com";
    public static final String DEFAULT_PHONE    = "+15550001111";
    public static final String DEFAULT_PASSWORD = "SecurePass123!";
    public static final String DEFAULT_CITY     = "Mumbai";

    public static final String ADMIN_EMAIL    = "admin@booking.com";
    public static final String ADMIN_PHONE    = "+15550002222";
    public static final String ADMIN_PASSWORD = "AdminPass456!";

    /** BCrypt strength 4 — safe for tests, fast. */
    private static final BCryptPasswordEncoder FAST_ENCODER = new BCryptPasswordEncoder(4);

    private UserFixtures() {}

    public static RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(DEFAULT_EMAIL);
        r.setPhone(DEFAULT_PHONE);
        r.setPassword(DEFAULT_PASSWORD);
        r.setCity(DEFAULT_CITY);
        return r;
    }

    public static RegisterRequest registerRequest(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPhone(phone);
        r.setPassword(password);
        r.setCity(DEFAULT_CITY);
        return r;
    }

    public static LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail(DEFAULT_EMAIL);
        r.setPassword(DEFAULT_PASSWORD);
        return r;
    }

    public static LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    /** Builds a persisted-style {@link User} with a fast BCrypt hash. */
    public static User user() {
        return User.builder()
                .email(DEFAULT_EMAIL)
                .phone(DEFAULT_PHONE)
                .passwordHash(FAST_ENCODER.encode(DEFAULT_PASSWORD))
                .role(UserRole.CUSTOMER)
                .city(DEFAULT_CITY)
                .build();
    }

    public static User user(String email, String phone, String rawPassword, UserRole role) {
        return User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(FAST_ENCODER.encode(rawPassword))
                .role(role)
                .city(DEFAULT_CITY)
                .build();
    }

    /** Injects a UUID via reflection (simulates a saved entity). */
    public static User savedUser() {
        User u = user();
        injectId(u, UUID.randomUUID());
        return u;
    }

    public static User savedUser(String email, String phone, String rawPassword, UserRole role) {
        User u = user(email, phone, rawPassword, role);
        injectId(u, UUID.randomUUID());
        return u;
    }

    private static void injectId(User user, UUID id) {
        try {
            var idField = com.booking.common.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject test UUID into User", e);
        }
    }
}
