package uk.gov.hmcts.darts.arm.client.version.fivetwo;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
    "darts.storage.arm-api.version5-2.authentication.auth-base-url=http://localhost:${wiremock.server.port}"
})
class ArmAuthClientIntTest extends IntegrationBaseWithWiremock {
    @Autowired
    private ArmAuthClient armAuthClient;

    private static final String TOKEN_PATH = "/account/token";

    @Test
    void getToken_ShouldSucceed_IfServerReturns200Success() {
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
        ArmTokenResponse token = armAuthClient.getToken(armTokenRequest);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                  .withRequestBody(equalTo("{\"username\":\"some-username\",\"password\":\"some-password\"}")));

        assertEquals("some-token", token.getAccessToken());
        assertEquals("some-token-type", token.getTokenType());
        assertEquals("some-expiry", token.getExpiresIn());
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads")
    void getToken_ShouldThrowException_IfServerReturns403Forbidden() throws InterruptedException {
        // Given
        stubFor(
            WireMock.post(urlEqualTo(TOKEN_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(403)));

        ArmTokenRequest armTokenRequest = createTokenRequest();
        Thread.sleep(2000);
        // When
        FeignException exception = assertThrows(FeignException.class, () -> armAuthClient.getToken(armTokenRequest));

        // Then
        assertEquals(
            "[403 Forbidden] during [POST] to [http://localhost:" + wiremockPort + "/account/token] [ArmAuthClient#getToken(ArmTokenRequest)]: []",
            exception.getMessage()
        );
    }

    private static ArmTokenRequest createTokenRequest() {
        return ArmTokenRequest.builder()
            .username("some-username")
            .password("some-password")
            .build();
    }
}
