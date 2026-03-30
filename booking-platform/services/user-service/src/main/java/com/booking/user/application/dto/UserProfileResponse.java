package com.booking.user.application.dto;

import com.booking.user.domain.entity.UserRole;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for {@code GET /v1/users/me}.
 * Never includes the password hash.
 */
@Value
@Builder
public class UserProfileResponse {

    UUID          id;
    String        email;
    String        phone;
    UserRole      role;
    String        city;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
