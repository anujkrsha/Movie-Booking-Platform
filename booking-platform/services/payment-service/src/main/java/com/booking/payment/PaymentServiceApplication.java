package com.booking.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Payment Service — entry point.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Payment initiation and callback handling (Razorpay / Stripe / UPI)</li>
 *   <li>Immutable transaction ledger</li>
 *   <li>Refund processing and status tracking</li>
 *   <li>Payment method management (cards, wallets)</li>
 *   <li>Webhook verification for gateway callbacks</li>
 * </ul>
 *
 * Default port: {@code 8086}
 */
@SpringBootApplication(scanBasePackages = {"com.booking.payment", "com.booking.common"})
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
