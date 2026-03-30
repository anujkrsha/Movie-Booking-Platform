package com.booking.user.infrastructure.security;

import com.booking.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Checks the Redis access-token blacklist before the main {@link com.booking.common.security.JwtAuthFilter}.
 *
 * <p>On logout, the access token's JTI is stored in Redis with a TTL equal to the
 * token's remaining lifetime. This filter intercepts any request carrying a blacklisted
 * token and returns 401 immediately, preventing the main filter from granting access.
 *
 * <p>Registered in {@link com.booking.user.web.config.UserSecurityConfig} <em>before</em>
 * {@code JwtAuthFilter} in the filter chain.
 */
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtService   jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtService.isBlacklisted(token)) {
            log.debug("Rejected blacklisted token for {}", request.getRequestURI());
            writeUnauthorized(response, request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String path) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Token has been revoked. Please log in again.",
                path
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
