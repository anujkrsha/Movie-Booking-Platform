package com.booking.movie.web.config;

import com.booking.common.security.JwtAuthFilter;
import com.booking.common.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for movie-service.
 *
 * <p>Access rules:
 * <ul>
 *   <li>{@code GET /v1/movies/**} — public (no token required).</li>
 *   <li>{@code POST /v1/movies}, {@code PUT /v1/movies/**}
 *       — require {@code ROLE_PLATFORM_ADMIN}.</li>
 *   <li>Actuator + Swagger — always public.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class MovieSecurityConfig {

    private final JwtAuthFilter              jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                // Public read-only endpoints
                .requestMatchers(HttpMethod.GET, "/v1/movies/**").permitAll()
                // Admin-only write endpoints
                .requestMatchers(HttpMethod.POST, "/v1/movies").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/v1/movies/**").hasRole("PLATFORM_ADMIN")
                // Infrastructure
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**",
                                 "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Returns RFC 7807 Problem Details JSON on access-denied (403) responses,
     * consistent with {@link JwtAuthenticationEntryPoint}'s 401 format.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Forbidden","status":403,\
                    "detail":"You do not have permission to perform this action.",\
                    "instance":"%s"}""".formatted(request.getRequestURI()));
        };
    }
}
