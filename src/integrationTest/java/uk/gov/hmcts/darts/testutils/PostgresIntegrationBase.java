package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

/**
 * Base class for integration tests running against a containerized Postgres with Testcontainers.
 */
@SpringBootTest
@ActiveProfiles({"intTest"})
public class PostgresIntegrationBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    protected MemoryLogAppender logAppender = LogUtil.getMemoryLogger();

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        "postgres:15-alpine"
    ).withDatabaseName("darts");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    static {
        // container will be automatically stopped
        POSTGRES.start();
    }

    @BeforeEach
    void clearDb() {
        dartsDatabase.clearDatabaseInThisOrder();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
    }
}