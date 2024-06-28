package uk.gov.hmcts.darts.testutils.stubs.wiremock;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.awaitility.Awaitility.await;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class DartsGatewayStub {

    public static final String DAR_NOTIFY_PATH = "/events/dar-notify";

    public void darNotificationReturnsSuccess() {
        stubFor(post(urlEqualTo(DAR_NOTIFY_PATH))
                        .willReturn(aResponse().withStatus(200).withBody("")));
    }

    public void darNotificationReturnsGatewayTimeoutError() {
        stubFor(post(urlEqualTo(DAR_NOTIFY_PATH)).willReturn(aResponse().withStatus(504)));
    }

    public void verifyDoesntReceiveDarEvent() {
        wait(1000);
        verify(exactly(0), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH)));
    }

    public void waitForRequestCount(int count) {
        await().until(() -> {
            try {
                verify(exactly(count), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH)));
                return true;
            } catch (VerificationException ex) {
                return false;
            }
        });
    }

    public void verifyReceivedNotificationType(int type) {
        var notificationType = "\"notification_type\":\"" + type + "\"";
        verify(exactly(1), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH))
                .withRequestBody(containing(notificationType)));
    }

    public void verifyNotificationUrl(String url, int count) {
        var notificationUrl = "\"notification_url\":\"" + url + "\"";
        verify(exactly(count), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH))
                .withRequestBody(containing(notificationUrl)));
    }

    public void clearStubs() {
        WireMock.reset();
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    private static void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
