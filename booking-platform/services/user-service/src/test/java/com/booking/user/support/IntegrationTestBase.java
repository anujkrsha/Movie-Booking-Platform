package com.booking.user.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests.
 *
 * <h3>Container reuse</h3>
 * <p>Both {@link PostgreSQLContainer} and {@link RedisContainer} are declared as
 * {@code static} fields — Testcontainers starts them once per JVM and reuses them
 * across all subclasses, dramatically reducing test-suite startup time.
 *
 * <h3>Dynamic property injection</h3>
 * <p>{@code @DynamicPropertySource} overwrites {@code spring.datasource.url},
 * {@code spring.data.redis.host}, and {@code spring.data.redis.port} with the
 * ports assigned by the container runtime.
 *
 * <h3>Profile</h3>
 * <p>Activate profile {@code integration-test} so integration-test-specific beans
 * (e.g. a no-op mail sender) can be conditionally declared.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("userdb")
                    .withUsername("booking")
                    .withPassword("booking")
                    .withReuse(true);

    @Container
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",  POSTGRES::getUsername);
        registry.add("spring.datasource.password",  POSTGRES::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        // Use fast ddl-auto for integration tests; real schema created by DDL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
