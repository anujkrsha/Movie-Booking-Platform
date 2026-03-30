package com.booking.movie.domain.entity;

/**
 * Lifecycle status of a movie on the platform.
 *
 * <ul>
 *   <li>{@code UPCOMING}  — announced, not yet in theatres.</li>
 *   <li>{@code RELEASED}  — currently showing; eligible for booking searches.</li>
 *   <li>{@code ARCHIVED}  — no longer showing; hidden from public search.</li>
 * </ul>
 */
public enum MovieStatus {
    UPCOMING,
    RELEASED,
    ARCHIVED
}
