package uk.gov.hmcts.darts.arm.client;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * At time of writing, ArmTokenClient has been written in isolation in preparation for dependant
 * tickets DMP-1911 and DMP-1912. It currently has no callers, so this integration test is the only
 * way to verify functionality.
 *
 * <p>If you're reading this and ArmTokenClient now has a caller, consider removing this test in favour
 * of a broader integration test.
 */
@TestPropertySource(properties = {
    "darts.storage.arm.token-base-url=http://localhost:8080"
})
class ArmClientIntTest extends IntegrationBase {

    @Autowired
    private ArmTokenClient armTokenClient;

    @Autowired
    private WireMockServer wireMockServer;

    private static final String TOKEN_PATH = "/api/v1/token";

    @Test
    void getTokenShouldSucceedIfServerReturns200Success() {
        // Given
        stubFor(
            WireMock.get(urlEqualTo(TOKEN_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(
                            """
                                {
                                    "access_token": "some-token",
                                    "token_type": "some-token-type",
                                    "expires_in": "some-expiry"
                                }
                                """
                        )
                        .withStatus(200)));

        ArmTokenRequest armTokenRequest = createTokenRequest();

        // When
        ArmTokenResponse token = armTokenClient.getToken(armTokenRequest);

        // Then
        wireMockServer.verify(getRequestedFor(urlEqualTo(TOKEN_PATH))
                                  .withRequestBody(equalTo("grant_type=password&username=some-username&password=some-password")));

        assertEquals("some-token", token.getAccessToken());
        assertEquals("some-token-type", token.getTokenType());
        assertEquals("some-expiry", token.getExpiresIn());
    }

    @Test
    void getTokenShouldThrowExceptionIfServerReturns403Forbidden() {
        // Given
        stubFor(
            WireMock.get(urlEqualTo(TOKEN_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(403)));

        ArmTokenRequest armTokenRequest = createTokenRequest();

        // When
        FeignException exception = assertThrows(FeignException.class, () -> armTokenClient.getToken(armTokenRequest));

        // Then
        assertEquals("[403 Forbidden] during [GET] to [http://localhost:8080/api/v1/token] [ArmTokenClient#getToken(ArmTokenRequest)]: []",
                     exception.getMessage());
    }

    private static ArmTokenRequest createTokenRequest() {
        return new ArmTokenRequest("some-username", "some-password", "password");
    }

}
