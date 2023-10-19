package uk.gov.hmcts.darts.testutils.stubs.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class BankHolidayApiStub {

    public static final String BANK_HOLIDAY_API_PATH = "/bank-holidays.json";

    public void returns(String bankHolidaysJson) {
        stubFor(get(urlEqualTo(BANK_HOLIDAY_API_PATH))
              .willReturn(aResponse()
                              .withHeader("Content-type", "application/json")
                              .withStatus(200)
                              .withBody(bankHolidaysJson)));
    }

    //    public void verifyNoRequestReceived() {
    //        verify(exactly(0), getRequestedFor(urlEqualTo(BANK_HOLIDAY_API_PATH)));
    //    }
    //
    //    public void verifyRequestReceived() {
    //        verify(exactly(1), getRequestedFor(urlEqualTo(BANK_HOLIDAY_API_PATH)));
    //    }
    //
    //    public void clearStubs() {
    //        WireMock.reset();
    //    }
    //
    //    private static void wait(int millis) {
    //        try {
    //            Thread.sleep(millis);
    //        } catch (InterruptedException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }
}
