package uk.gov.hmcts.darts.testutils;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

@AutoConfigureWireMock(port = 10300, files = "file:src/integrationTest/resources/wiremock")
public class IntegrationBaseWithGatewayStub extends IntegrationBase {

    @Value("${wiremock.server.port}")
    protected String wiremockPort;


    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected DartsGatewayStub dartsGateway;

    @BeforeEach
    void clearStubs() {
        dartsGateway.clearStubs();
    }
}