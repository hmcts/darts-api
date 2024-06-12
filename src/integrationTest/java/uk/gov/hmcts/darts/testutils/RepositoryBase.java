package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

@SpringBootTest
@ActiveProfiles({"intTest", "postgres"})
public class RepositoryBase {

    static {
        PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));
        postgres = postgres.withDatabaseName("darts");
        postgres = postgres.withSharedMemorySize(128_000_000L);
        postgres.start();

        System.setProperty("spring.datasource.port", postgres.getMappedPort(5432).toString());
    }

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    protected MemoryLogAppender logAppender = LogUtil.getMemoryLogger();

    @BeforeEach
    void clearDb() {
        dartsDatabase.clearDatabaseInThisOrder();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
    }
}