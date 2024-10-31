package uk.gov.hmcts.darts.testutils.stubs.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class TokenStub {

    @SuppressWarnings("checkstyle:linelength")
    public void stubExternalJwksKeys(String keys) {
        stubFor(get(urlPathEqualTo("/discovery/v2.0/keys"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                                      {"keys":[%s]}
                                                      """.formatted(keys))));
    }
}