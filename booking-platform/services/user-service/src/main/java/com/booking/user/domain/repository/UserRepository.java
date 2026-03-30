package com.booking.user.domain.repository;

import com.booking.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link User} entities.
 *
 * <p>Both {@code findByEmail} and {@code findByPhone} use the unique indexes defined
 * on the {@code users} table, so these are single-row index scans.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
