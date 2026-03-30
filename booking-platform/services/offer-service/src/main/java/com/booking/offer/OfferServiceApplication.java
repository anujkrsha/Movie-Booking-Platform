package com.booking.offer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Offer Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Promo code creation, validation, and redemption</li>
 *   <li>Discount rule engine (percentage, flat, buy-X-get-Y)</li>
 *   <li>Per-user redemption limits and expiry enforcement</li>
 *   <li>Loyalty points accrual and redemption</li>
 *   <li>Offer eligibility checks (called by booking-service)</li>
 * </ul>
 *
 * Default port: {@code 8087}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.offer", "com.booking.common"})
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class OfferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
    }
}
