package com.booking.theatre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Theatre Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Theatre registration and management (name, address, city)</li>
 *   <li>Screen configuration (name, capacity, screen type: 2D / 3D / IMAX)</li>
 *   <li>Seat layout definition per screen (row, column, category: NORMAL / PREMIUM / RECLINER)</li>
 *   <li>Amenities and facilities metadata</li>
 * </ul>
 *
 * Default port: {@code 8083}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.theatre", "com.booking.common"})
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class TheatreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheatreServiceApplication.class, args);
    }
}
