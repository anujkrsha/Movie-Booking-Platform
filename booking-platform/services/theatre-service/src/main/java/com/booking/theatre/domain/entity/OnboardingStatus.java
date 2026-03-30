package com.booking.theatre.domain.entity;

/**
 * Lifecycle state of a theatre partner's onboarding.
 *
 * <ul>
 *   <li>{@code PENDING}   — application submitted; awaiting platform review.</li>
 *   <li>{@code APPROVED}  — live on the platform; shows can be listed.</li>
 *   <li>{@code SUSPENDED} — temporarily de-listed (compliance / payment issue).</li>
 * </ul>
 */
public enum OnboardingStatus {
    PENDING,
    APPROVED,
    SUSPENDED
}
