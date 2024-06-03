package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

@AutoConfigureWireMock(files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@ActiveProfiles({"intTest", "postgres", "in-memory-caching"})
public class IntegrationBase {
    public static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withUsername("test").withPassword("test").withDatabaseName("darts");

    static {
        GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
        redis.start();
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }

    @Autowired
    protected DartsDatabaseStub dartsDatabase;
    @Autowired
    protected ObjectMapper objectMapper;

    protected MemoryLogAppender logAppender = LogUtil.getMemoryLogger();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.port", () -> postgres.getMappedPort(5432).toString());
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