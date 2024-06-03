package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

@AutoConfigureWireMock(port = 8070, files = "file:src/integrationTest/resources/wiremock")
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"intTest", "postgres", "in-memory-caching"})
public class IntegrationPerClassBase {

    public static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withUsername("test").withPassword("test").withDatabaseName("darts");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.port", () -> postgres.getMappedPort(5432).toString());
    }

    protected DartsGatewayStub dartsGateway = new DartsGatewayStub();

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @BeforeAll
    void clearDbInThisOrder() {
        dartsDatabase.clearDatabaseInThisOrder();
        dartsGateway.clearStubs();
    }
}