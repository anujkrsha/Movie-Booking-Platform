package com.booking.common.infra.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Kafka consumer configuration.
 *
 * <h3>Manual offset commit</h3>
 * <p>Ack mode is set to {@link ContainerProperties.AckMode#MANUAL_IMMEDIATE}, meaning
 * consumers must call {@code Acknowledgment.acknowledge()} explicitly after successful
 * processing. This prevents message loss on listener crashes and gives full control over
 * re-delivery.
 *
 * <h3>Error handling with DLQ</h3>
 * <p>The {@link DefaultErrorHandler} is configured with:
 * <ul>
 *   <li>3 retry attempts (4 total deliveries) with a 1-second fixed back-off.</li>
 *   <li>A {@link DeadLetterPublishingRecoverer} that routes poison messages to
 *       {@code <original-topic>.DLT} (e.g. {@code booking-events.DLT}) with the
 *       original partition preserved for ordered replay.</li>
 * </ul>
 *
 * <h3>Deserialisation safety</h3>
 * <p>{@link ErrorHandlingDeserializer} wraps the {@link JsonDeserializer} so a
 * malformed message does not crash the entire partition — it is routed straight to
 * the DLT without retries.
 */
@Configuration
@EnableKafka
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Consumer group ID defaults to the application name so each service gets its own group.
     * Override via {@code spring.kafka.consumer.group-id} in the service's application.yml.
     */
    @Value("${spring.kafka.consumer.group-id:${spring.application.name:default-group}}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Manual offset commit — do not auto-commit
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // How many records to fetch per poll; keep modest to limit reprocessing on restart
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

        // ── Deserialiser: ErrorHandlingDeserializer wraps JsonDeserializer ────
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Deserialise to Map by default; @KafkaListener methods can declare the concrete type
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.booking.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory with:
     * <ul>
     *   <li>Manual ack mode.</li>
     *   <li>Concurrency of 3 threads per listener (tune per service via property override).</li>
     *   <li>DLQ error handler — 3 retries, 1-second back-off, then publish to {@code *.DLT}.</li>
     * </ul>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Error handler: 3 retries with 1-second fixed back-off, then dead-letter the record.
     *
     * <p>DLT topic is named {@code <original>.DLT} automatically by
     * {@link DeadLetterPublishingRecoverer}. The same partition index is preserved so
     * ordered replay is possible.
     */
    private DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // 3 retries, 1 000 ms apart → 4 total delivery attempts
        FixedBackOff backOff = new FixedBackOff(1_000L, 3L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Deserialization failures go straight to DLT — no point retrying them
        handler.addNotRetryableExceptions(org.apache.kafka.common.errors.SerializationException.class);
        return handler;
    }
}
