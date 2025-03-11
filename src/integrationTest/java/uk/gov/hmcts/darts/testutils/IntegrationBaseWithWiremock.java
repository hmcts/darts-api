package uk.gov.hmcts.darts.testutils;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.DartsGatewayStub;

import java.time.Duration;

// port = 0 enables random ports as recommended by Wiremock. Tests will be faster and more reliable
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@Slf4j
public class IntegrationBaseWithWiremock extends IntegrationBase {

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    @Autowired
    protected DartsGatewayStub dartsGateway;

    @Autowired
    protected WireMockServer wireMockServer;

    @BeforeEach
    void setup() {
        try {
            log.info("Wiremock Port: " + wiremockPort);
            dartsGateway.clearStubs();
            wireMockServer.start();
            //Wait required to ensure that the wiremock server is up and running before the tests start
            waitForWiremock(10, Duration.ofSeconds(2));
            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(DartsTokenGenerator.getGlobalKey());
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }
    }

    @SuppressWarnings("PMD.DoNotUseThreads")//We need to wait for the wiremock server to start
    private void waitForWiremock(long waitMs, Duration maxTimeout) throws Exception {
        long maxWaitMs = maxTimeout.toMillis();
        while (!isWireMockRunning()) {
            Thread.sleep(waitMs);
            maxWaitMs -= waitMs;
            if (maxWaitMs <= 0) {
                throw new Exception("Wiremock server did not start within the timeout");
            }
        }
    }

    private boolean isWireMockRunning() {
        try {
            if (!wireMockServer.isRunning()) {
                return false;
            }
            HttpUriRequest request = new HttpGet("http://localhost:" + wiremockPort + "/__admin/mappings");
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            if (HttpStatus.valueOf(httpResponse.getCode()).is2xxSuccessful()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}