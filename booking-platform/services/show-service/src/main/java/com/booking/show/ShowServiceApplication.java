package com.booking.show;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Show Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Show scheduling (movie + screen + date-time + language + format)</li>
 *   <li>Real-time seat availability tracking per show</li>
 *   <li>Dynamic pricing (base price + surge + category multipliers)</li>
 *   <li>Scheduled job to expire unconfirmed seat locks</li>
 * </ul>
 *
 * Default port: {@code 8084}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.show", "com.booking.common"})
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
public class ShowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowServiceApplication.class, args);
    }
}
