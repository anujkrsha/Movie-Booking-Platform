package com.booking.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Movie Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Movie catalogue (title, description, genre, language, duration)</li>
 *   <li>Cast and crew metadata</li>
 *   <li>User ratings and reviews</li>
 *   <li>Currently showing / upcoming filters</li>
 * </ul>
 *
 * Default port: {@code 8082}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.movie", "com.booking.common"})
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}
