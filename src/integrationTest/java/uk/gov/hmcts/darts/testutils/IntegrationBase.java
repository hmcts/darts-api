package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

@AutoConfigureWireMock(files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase {

    static {
        GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
        redis.start();
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }

    @Autowired
    protected OpenInViewUtil openInViewUtil;
    @Autowired
    protected DartsDatabaseStub dartsDatabase;
    @Autowired
    protected ObjectMapper objectMapper;

    protected MemoryLogAppender logAppender = LogUtil.getMemoryLogger();

    @BeforeEach
    void clearDb() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        dartsDatabase.clearDatabaseInThisOrder();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
    }
}