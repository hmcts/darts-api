package uk.gov.hmcts.darts.testutils;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests running against a containerized Postgres with Testcontainers.
 */
@SpringBootTest
@ActiveProfiles({"intTest", "in-memory-caching"})
@Import(IntegrationTestConfiguration.class)
public class PostgresIntegrationBase extends TestBase {

    /**
     * We shouldn't need to change this value. If we need to increase the limit as more tests use PostgresIntegrationBase, then it suggests we have a
     * connection leak somewhere and that should be investigated.
     */
    private static final int SERVER_MAX_CONNECTIONS = 50;

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("hmctspublic.azurecr.io/imported/postgres")
                .asCompatibleSubstituteFor("postgres")
        ).withDatabaseName("darts")
            .withUsername("darts")
            .withPassword("darts");
        POSTGRES.setCommand("postgres", "-c", String.format("max_connections=%d", SERVER_MAX_CONNECTIONS));

        // container will be automatically stopped
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
