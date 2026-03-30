package com.booking.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Email dispatch via JavaMailSender with Thymeleaf HTML templates</li>
 *   <li>SMS notifications (Twilio / AWS SNS — pluggable provider)</li>
 *   <li>Push notifications (Firebase FCM)</li>
 *   <li>Notification delivery status tracking and retry logic</li>
 *   <li>User notification preferences management</li>
 * </ul>
 *
 * Default port: {@code 8089}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.notification", "com.booking.common"})
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
