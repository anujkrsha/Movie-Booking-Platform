package com.booking.theatre.domain.entity;

import com.booking.common.entity.BaseEntity;
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

import java.util.UUID;

/**
 * A theatre (cinema hall) owned by a partner on the platform.
 *
 * <p>Indexed columns:
 * <ul>
 *   <li>{@code city}              — primary browse / geo filter.</li>
 *   <li>{@code partner_id}        — lists all theatres for a given theatre admin.</li>
 *   <li>{@code onboarding_status} — operational filter (approved vs pending/suspended).</li>
 * </ul>
 */
@Entity
@Table(
    name = "theatres",
    indexes = {
        @Index(name = "idx_theatre_city",              columnList = "city"),
        @Index(name = "idx_theatre_partner_id",        columnList = "partner_id"),
        @Index(name = "idx_theatre_onboarding_status", columnList = "onboarding_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theatre extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", nullable = false, length = 512)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /** GST registration number for tax invoicing. */
    @Column(name = "gst_number", nullable = false, length = 20, unique = true)
    private String gstNumber;

    /** Opaque reference to the partner's bank account in the payment ledger. */
    @Column(name = "bank_account_ref", length = 128)
    private String bankAccountRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    /** UUID of the {@code User} (THEATRE_ADMIN role) who owns this theatre. */
    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;
}
