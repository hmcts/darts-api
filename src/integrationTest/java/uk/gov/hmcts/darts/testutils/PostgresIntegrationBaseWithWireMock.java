package uk.gov.hmcts.darts.testutils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

/**
 * A reimplementation of IntegrationBaseWithWiremock, but for Postgres.
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@Slf4j
public class PostgresIntegrationBaseWithWireMock extends PostgresIntegrationBase {

    @Autowired
    protected DartsGatewayStub dartsGateway;

    @BeforeEach
    @SuppressWarnings("PMD.DoNotUseThreads")
    void setup() {
        try {
            dartsGateway.clearStubs();
            //Wait required to ensure that the wiremock server is up and running before the tests start
            // The approach here has been adopted from IntegrationBaseWithWiremock, but this will introduce a 2 second delay before each and every test which
            // will extend the runtime duration of our test suite. Should really find a better way to ensure wiremock is started before the tests start.
            Thread.sleep(2000);
            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(DartsTokenGenerator.getGlobalKey());
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }
    }

}
