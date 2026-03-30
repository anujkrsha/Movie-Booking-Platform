package com.booking.user.domain.entity;

/**
 * Platform roles assigned to a registered user.
 *
 * <ul>
 *   <li>{@code CUSTOMER}        — regular ticket buyer.</li>
 *   <li>{@code THEATRE_ADMIN}   — manages theatres/screens for a specific partner.</li>
 *   <li>{@code PLATFORM_ADMIN}  — full back-office access.</li>
 * </ul>
 */
public enum UserRole {
    CUSTOMER,
    THEATRE_ADMIN,
    PLATFORM_ADMIN
}
