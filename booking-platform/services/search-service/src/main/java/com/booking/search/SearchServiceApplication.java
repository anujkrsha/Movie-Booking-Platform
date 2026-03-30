package com.booking.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Search Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Full-text search for movies, shows, and theatres (JPQL / native PostgreSQL {@code ILIKE})</li>
 *   <li>Faceted filters: city, date, genre, language, format, price range</li>
 *   <li>Autocomplete suggestions</li>
 *   <li>Trending and popular content aggregation</li>
 * </ul>
 *
 * <p><strong>Elasticsearch upgrade path:</strong> replace the JPA repositories with
 * {@code spring-boot-starter-data-elasticsearch} and keep the service API unchanged.
 *
 * Default port: {@code 8088}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.search", "com.booking.common"})
@EnableJpaAuditing
@EnableCaching
@ConfigurationPropertiesScan
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
