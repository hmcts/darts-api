package uk.gov.hmcts.darts.event.testutils;

import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class DartsGatewayStub {

    public static final String DAR_NOTIFY_PATH = "/events/dar-notify";

    public void darNotifyIsUp() {
        stubFor(post(urlEqualTo(DAR_NOTIFY_PATH))
            .willReturn(aResponse().withStatus(200).withBody("")));
    }

    public void verifyDoesntReceiveDarEvent() {
        verify(exactly(0), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH)));
    }

    public void verifyReceivedNotificationType(int type) {
        var notificationType = "\"notification_type\":\"" + type + "\"";

        verify(exactly(1), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH))
            .withRequestBody(containing(notificationType)));
    }

    public void clearStubs() {
        WireMock.reset();
    }
}
