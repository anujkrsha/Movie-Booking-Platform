package com.booking.common.infra.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Kafka producer configuration.
 *
 * <p>Key reliability settings:
 * <ul>
 *   <li>{@code enable.idempotence=true}  — exactly-once delivery at the producer level;
 *       Kafka deduplicates retried sends within a single producer session.</li>
 *   <li>{@code acks=all}                 — leader waits for all in-sync replicas to
 *       acknowledge before returning success (requires {@code min.insync.replicas ≥ 2}
 *       in production clusters).</li>
 *   <li>{@code retries=3}               — up to 3 automatic retries on transient errors.</li>
 *   <li>{@code max.in.flight.requests.per.connection=5} — maximum safe value when
 *       idempotence is enabled.</li>
 * </ul>
 *
 * <p>Values are serialised to JSON via {@link JsonSerializer} with type headers disabled
 * — consumers use {@link org.springframework.kafka.support.serializer.JsonDeserializer}
 * with an explicit target type, which is safer than trusting headers from producers.
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // ── Reliability settings ──────────────────────────────────────────────
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // ── Performance / batching ────────────────────────────────────────────
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);           // 16 KB
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);                // wait up to 5 ms to batch
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");  // cpu-cheap + good ratio

        // Disable type headers — consumers rely on explicit target-type config, not headers
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Shared {@link KafkaTemplate} used by all producer services.
     * Autowire this bean (or qualify it) wherever you need to publish events.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
