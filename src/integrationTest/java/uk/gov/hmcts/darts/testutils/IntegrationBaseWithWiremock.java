package uk.gov.hmcts.darts.testutils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

// port = 0 enables random ports as recommended by Wiremock. Tests will be faster and more reliable
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@Slf4j
public class IntegrationBaseWithWiremock extends IntegrationBase {

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    @Autowired
    protected DartsGatewayStub dartsGateway;

    @BeforeEach
    @SuppressWarnings("PMD.DoNotUseThreads")
    void setup() {
        try {
            log.info("Wiremock Port: " + wiremockPort);
            dartsGateway.clearStubs();
            //Wait required to ensure that the wiremock server is up and running before the tests start
            Thread.sleep(2000);
            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(DartsTokenGenerator.getGlobalKey());
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }
    }
}