package com.booking.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /v1/auth/logout}.
 *
 * <p>The client must send the access token it wishes to invalidate.
 * The server will also delete the associated refresh token from Redis.
 */
@Data
public class LogoutRequest {

    /**
     * The access token to blacklist.
     * Alternatively, the server can read this from the {@code Authorization} header —
     * but accepting it in the body allows explicit single-device logout.
     */
    @NotBlank(message = "Access token is required")
    private String accessToken;
}
