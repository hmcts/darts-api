package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

@AutoConfigureWireMock(port = 8070, files = "file:src/integrationTest/resources/wiremock")
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationPerClassBase {

    protected DartsGatewayStub dartsGateway = new DartsGatewayStub();

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @BeforeAll
    void clearDbInThisOrder() {
        dartsDatabase.clearDatabaseInThisOrder();
        dartsGateway.clearStubs();
    }
}
