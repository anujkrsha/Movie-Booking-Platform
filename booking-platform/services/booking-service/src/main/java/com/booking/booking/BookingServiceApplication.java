package com.booking.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Booking Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Seat selection and temporary locking (TTL-based)</li>
 *   <li>Booking creation and lifecycle management (PENDING → CONFIRMED → CANCELLED)</li>
 *   <li>Ticket generation and QR code embedding</li>
 *   <li>Cancellation and refund initiation (calls payment-service)</li>
 *   <li>Booking history per user</li>
 * </ul>
 *
 * Default port: {@code 8085}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.booking", "com.booking.common"})
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
