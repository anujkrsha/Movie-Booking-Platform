package com.booking.user.infrastructure.security;

import com.booking.common.exception.ResourceNotFoundException;
import com.booking.common.security.JwtProperties;
import com.booking.common.security.JwtUtil;
import com.booking.user.domain.entity.User;
import com.booking.user.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Production implementation of {@link JwtService} that resolves email and role
 * from the database during refresh-token rotation.
 *
 * <p>A DB lookup on refresh is acceptable — refresh is an infrequent operation
 * (at most once per 15-minute access-token window).
 *
 * <p>Declared {@code @Primary} so Spring injects this bean wherever
 * {@code JwtService} is required.
 */
@Primary
@Service
public class DefaultJwtService extends JwtService {

    private final UserRepository userRepository;

    public DefaultJwtService(
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            UserRepository userRepository) {
        super(jwtUtil, jwtProperties, redisTemplate);
        this.userRepository = userRepository;
    }

    @Override
    String lookupEmailForUser(UUID userId) {
        return findUser(userId).getEmail();
    }

    @Override
    String lookupRoleForUser(UUID userId) {
        return findUser(userId).getRole().name();
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
