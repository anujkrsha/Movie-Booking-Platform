package com.booking.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * User Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>User registration and login (issues JWT)</li>
 *   <li>Profile management (name, avatar, preferences)</li>
 *   <li>Role management (USER, ADMIN, THEATRE_ADMIN)</li>
 *   <li>Password reset / email verification</li>
 * </ul>
 *
 * Default port: {@code 8081}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.user", "com.booking.common"})
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
