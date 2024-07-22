package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

public class IntegrationBaseWithGatewayStub extends IntegrationBase {

    //TODO: change to use extension
    protected DartsGatewayStub dartsGateway = new DartsGatewayStub();

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void clearStubs() {
        dartsGateway.clearStubs();
    }
}
