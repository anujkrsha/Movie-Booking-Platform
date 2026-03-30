package com.booking.user.web.config;

import com.booking.common.security.JwtAuthFilter;
import com.booking.common.security.JwtAuthenticationEntryPoint;
import com.booking.user.infrastructure.security.JwtService;
import com.booking.user.infrastructure.security.TokenBlacklistFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.booking.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * User-service security configuration.
 *
 * <p>Overrides the default {@link com.booking.common.security.SecurityConfig} filter chain
 * (the default is {@code @ConditionalOnMissingBean}) with user-service-specific URL rules.
 *
 * <h3>Filter order (top to bottom, each request passes through all matching filters)</h3>
 * <ol>
 *   <li>{@link TokenBlacklistFilter} — rejects blacklisted access tokens before JWT parsing.</li>
 *   <li>{@link JwtAuthFilter}         — validates RS256 signature, populates SecurityContext.</li>
 * </ol>
 *
 * <h3>Public endpoints</h3>
 * <ul>
 *   <li>{@code POST /v1/auth/register}</li>
 *   <li>{@code POST /v1/auth/login}</li>
 *   <li>{@code POST /v1/auth/refresh}</li>
 *   <li>{@code GET  /actuator/health}</li>
 *   <li>Swagger / OpenAPI endpoints</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class UserSecurityConfig {

    private final JwtAuthFilter               jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtService                  jwtService;
    private final ObjectMapper                objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Public auth endpoints ─────────────────────────
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                        // ── Actuator / docs ────────────────────────────────
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // ── Everything else requires a valid JWT ───────────
                        .anyRequest().authenticated()
                )
                // Blacklist check must come BEFORE JWT validation
                .addFilterBefore(tokenBlacklistFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public TokenBlacklistFilter tokenBlacklistFilter() {
        return new TokenBlacklistFilter(jwtService, objectMapper);
    }

    /** BCrypt strength 12 — ~300 ms per hash, appropriate for production login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Returns RFC 7807 JSON instead of Spring Security's default HTML 403 page.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            ErrorResponse body = ErrorResponse.of(
                    HttpStatus.FORBIDDEN.value(),
                    "Forbidden",
                    "You do not have permission to access this resource",
                    request.getRequestURI()
            );
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), body);
        };
    }
}
