package uk.gov.hmcts.darts.testutils.stubs.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class BankHolidayApiStub {

    public static final String BANK_HOLIDAY_API_PATH = "/bank-holidays.json";

    public void returns(String bankHolidaysJson) {
        stubFor(get(urlEqualTo(BANK_HOLIDAY_API_PATH))
                    .willReturn(aResponse()
                                    .withHeader("Content-type", "application/json")
                                    .withStatus(200)
                                    .withBody(bankHolidaysJson)));
    }
}
