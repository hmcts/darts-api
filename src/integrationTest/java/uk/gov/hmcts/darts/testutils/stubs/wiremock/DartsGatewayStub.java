package uk.gov.hmcts.darts.testutils.stubs.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.stereotype.Component;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static uk.gov.hmcts.darts.test.common.AwaitabilityUtil.waitForMax10SecondsWithOneSecondPoll;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")

@Component
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

    public void verifyReceivedNotificationType(int type) {
        var notificationType = "\"notification_type\":\"" + type + "\"";
        waitForMax10SecondsWithOneSecondPoll(() ->
             verify(exactly(1), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH))
                 .withRequestBody(containing(notificationType))));
    }

    public void verifyNotificationUrl(String url, int count) {
        var notificationType = "\"notification_url\":\"" + url + "\"";
        waitForMax10SecondsWithOneSecondPoll(() ->
            verify(exactly(count), postRequestedFor(urlEqualTo(DAR_NOTIFY_PATH))
                    .withRequestBody(containing(notificationType))));
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