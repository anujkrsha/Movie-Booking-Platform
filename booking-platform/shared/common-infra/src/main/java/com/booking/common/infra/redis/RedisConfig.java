package com.booking.common.infra.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Shared Redis bean definitions, auto-configured via {@code AutoConfiguration.imports}.
 *
 * <p>Two templates are provided:
 * <ol>
 *   <li>{@code redisTemplate}        — {@code RedisTemplate<String, String>} for plain-text values
 *       (seat-lock keys, simple flags, idempotency keys).</li>
 *   <li>{@code redisObjectTemplate}  — {@code RedisTemplate<String, Object>} for JSON-serialised
 *       objects (cached DTOs, session data). Uses Jackson with type metadata embedded so
 *       deserialisation is unambiguous.</li>
 * </ol>
 *
 * <p>Both templates share a {@link StringRedisSerializer} for keys, which keeps Redis keys
 * human-readable and tool-friendly (no binary prefixes).
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisConfig {

    /**
     * Plain string template — primary choice for seat-lock keys and other simple values.
     * No serialiser overhead; values are stored as raw UTF-8 strings.
     */
    @Bean("redisTemplate")
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * JSON object template — use for caching complex DTOs or domain objects.
     *
     * <p>The {@link ObjectMapper} is configured with:
     * <ul>
     *   <li>Java Time module — serialises {@code LocalDateTime}, {@code Instant}, etc.</li>
     *   <li>Default typing on non-final types — stores {@code @class} in the JSON so
     *       the deserialiser knows the concrete type without a type hint at the call-site.</li>
     * </ul>
     */
    @Bean("redisObjectTemplate")
    public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Dedicated {@link ObjectMapper} for Redis value serialisation.
     *
     * <p>A separate instance (rather than reusing the application-wide mapper) prevents
     * leaking the {@code activateDefaultTyping} setting into REST response serialisation.
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Embed @class so GenericJackson2JsonRedisSerializer can round-trip arbitrary objects
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
