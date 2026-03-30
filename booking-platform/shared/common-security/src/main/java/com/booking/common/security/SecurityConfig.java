package com.booking.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Default stateless JWT security configuration shared across all services.
 *
 * <h3>How to customise per-service</h3>
 * Declare your own {@link SecurityFilterChain} bean in the service module and
 * this default will be skipped ({@code @ConditionalOnMissingBean}).
 *
 * <pre>
 * // In user-service SecurityConfig
 * {@literal @}Bean
 * public SecurityFilterChain securityFilterChain(HttpSecurity http,
 *                                                 JwtAuthFilter jwtAuthFilter,
 *                                                 JwtAuthenticationEntryPoint entryPoint)
 *         throws Exception {
 *     return baseChain(http, jwtAuthFilter, entryPoint)
 *             .authorizeHttpRequests(auth -> auth
 *                     .requestMatchers("/api/v1/auth/**").permitAll()
 *                     .anyRequest().authenticated())
 *             .build();
 * }
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter               jwtAuthFilter;
    private final JwtAuthenticationEntryPoint entryPoint;

    /** Endpoints open to anonymous requests in every service. */
    private static final String[] PUBLIC_PATTERNS = {
            "/actuator/health",
            "/actuator/info",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Default filter chain — permits actuator + Swagger, requires JWT for everything else.
     * Automatically skipped if a service declares its own {@link SecurityFilterChain} bean.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return buildChain(http);
    }

    /**
     * Shared helper that services can call from their own
     * {@code securityFilterChain} bean to reuse the base configuration.
     */
    public SecurityFilterChain buildChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATTERNS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
