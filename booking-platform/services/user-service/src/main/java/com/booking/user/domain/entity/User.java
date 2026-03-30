package com.booking.user.domain.entity;

import com.booking.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registered platform user.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code email}  — unique; used on every authentication lookup.</li>
 *   <li>{@code phone}  — used for OTP / contact lookups.</li>
 *   <li>{@code role}   — used for RBAC-scoped queries (e.g. list all theatre admins).</li>
 *   <li>{@code city}   — used for geo-targeted promotions.</li>
 * </ul>
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email",  columnList = "email",  unique = true),
        @Index(name = "idx_user_phone",  columnList = "phone"),
        @Index(name = "idx_user_role",   columnList = "role"),
        @Index(name = "idx_user_city",   columnList = "city")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * BCrypt hash of the user's password.
     * Never serialised to JSON or included in API responses.
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "city", length = 100)
    private String city;
}
