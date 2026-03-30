package com.booking.user.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Returned on successful authentication (register / login / refresh).
 *
 * <pre>
 * {
 *   "access_token":  "eyJ...",
 *   "refresh_token": "eyJ...",
 *   "token_type":    "Bearer",
 *   "expires_in":    900
 * }
 * </pre>
 */
@Value
@Builder
public class TokenResponse {

    @JsonProperty("access_token")
    String accessToken;

    @JsonProperty("refresh_token")
    String refreshToken;

    @JsonProperty("token_type")
    @Builder.Default
    String tokenType = "Bearer";

    /**
     * Access-token validity in seconds (matches {@code jwt.expiration / 1000}).
     */
    @JsonProperty("expires_in")
    long expiresIn;
}
