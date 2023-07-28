package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsGatewayStub;

@AutoConfigureWireMock
@SpringBootTest
@ActiveProfiles({"local"})
public class IntegrationBase {

    protected DartsGatewayStub dartsGateway = new DartsGatewayStub();

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @BeforeEach
    void clearDbInThisOrder() {
        dartsDatabase.clearDatabase();
        dartsGateway.clearStubs();
    }
}
