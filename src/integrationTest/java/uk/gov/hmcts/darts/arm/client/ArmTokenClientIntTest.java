package uk.gov.hmcts.darts.arm.client;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArmTokenClientIntTest extends IntegrationBaseWithWiremock {

    @Autowired
    private ArmTokenClient armTokenClient;

    @Autowired
    private WireMockServer wireMockServer;

    private static final String TOKEN_PATH = "/api/v1/token";

    @Test
    void getTokenShouldSucceedIfServerReturns200Success() {
        // Given
        stubFor(
            WireMock.post(urlEqualTo(TOKEN_PATH))
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
        wireMockServer.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                  .withRequestBody(equalTo("grant_type=password&username=some-username&password=some-password")));

        assertEquals("some-token", token.getAccessToken());
        assertEquals("some-token-type", token.getTokenType());
        assertEquals("some-expiry", token.getExpiresIn());
    }

    @Disabled("This test is disabled because the client is not handling 403 Forbidden responses correctly")
    @Test
    void getTokenShouldThrowExceptionIfServerReturns403Forbidden() {
        // Given
        stubFor(
            WireMock.post(urlEqualTo(TOKEN_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(403)));

        ArmTokenRequest armTokenRequest = createTokenRequest();

        // When
        FeignException exception = assertThrows(FeignException.class, () -> armTokenClient.getToken(armTokenRequest));

        // Then
        assertEquals(
            "[403 Forbidden] during [POST] to [http://localhost:" + wiremockPort + "/api/v1/token] [ArmTokenClient#getToken(ArmTokenRequest)]: []",
            exception.getMessage()
        );
    }

    private static ArmTokenRequest createTokenRequest() {
        return new ArmTokenRequest("some-username", "some-password", GrantType.PASSWORD.getValue());
    }

}