package com.booking.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} implementation populated by {@link JwtAuthFilter}
 * from claims in the validated JWT.
 *
 * <p>Retrieve the authenticated principal in controllers via:
 * <pre>
 *   {@literal @}AuthenticationPrincipal UserPrincipal principal
 * </pre>
 * or:
 * <pre>
 *   (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    /** User's UUID — sourced from the JWT {@code sub} claim. */
    private UUID   id;

    /** Email address used as the Spring Security principal name (from the {@code email} claim). */
    private String username;

    private String email;

    /** Always {@code null} for JWT-authenticated requests. */
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
