package uk.gov.hmcts.darts.event.testutils;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

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