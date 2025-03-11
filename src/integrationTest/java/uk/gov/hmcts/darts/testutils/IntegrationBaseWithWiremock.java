package uk.gov.hmcts.darts.testutils;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
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
            // populate the jkws keys endpoint with a global public key
            tokenStub.stubExternalJwksKeys(DartsTokenGenerator.getGlobalKey());
            //Wait required to ensure that the wiremock server is up and running before the tests start
            waitForWiremock(10, Duration.ofSeconds(2));
        } catch (Exception e) {
            log.error("Error setting up wiremock", e);
        }
    }

    @SuppressWarnings("PMD.DoNotUseThreads")//We need to wait for the wiremock server to start
    private void waitForWiremock(long waitMs, Duration maxTimeout) throws InterruptedException {
        long maxWaitMs = maxTimeout.toMillis();
        while (!isWireMockRunning()) {
            Thread.sleep(waitMs);
            maxWaitMs -= waitMs;
            if (maxWaitMs <= 0) {
                throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR, "Wiremock server did not start");
            }
        }
    }

    private boolean isWireMockRunning() {
        try {
            if (!wireMockServer.isRunning()) {
                return false;
            }
            HttpUriRequest request = new HttpGet("http://localhost:" + wiremockPort + "/__admin/mappings");
            CloseableHttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            if (!HttpStatus.valueOf(httpResponse.getCode()).is2xxSuccessful()) {
                return false;
            }
            String res = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            return res.contains("/discovery/v2.0/keys");
        } catch (Exception e) {
            log.error("Error checking if wiremock is running", e);
            return false;
        }
    }
}