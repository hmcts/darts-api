package uk.gov.hmcts.darts.authentication.client.impl;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthClientImplTest {

    @Mock
    private AuthProviderConfigurationProperties providerConfigurationProperties;

    private OAuthClientImpl oAuthClientImpl;

    @BeforeEach
    void setUp() {
        oAuthClientImpl = new OAuthClientImpl();
    }

    @Test
    void fetchAccessToken_ShouldReturnResponseWhenAzureCallIsSuccessful() {
        // given
        HTTPResponse response = mockSuccessResponse();
        when(oAuthClientImpl.fetchAccessToken(any(), any(), any(), any(), any())).thenReturn(response);

        // when
        HTTPResponse result = oAuthClientImpl.fetchAccessToken(providerConfigurationProperties, "REFRESH_TOKEN", "CLIENT_ID", "CLIENT_SECRET", "SCOPE");

        // then
        assertEquals(HttpStatus.SC_OK, result.getStatusCode());
        assertEquals("{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}", result.getContent());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void fetchAccessToken_ShouldThrowExceptionWhenRefreshTokenIsBlankOrNull(String refreshToken) {
        // when
        assertThrows(IllegalArgumentException.class,
                     () -> oAuthClientImpl.fetchAccessToken(providerConfigurationProperties, refreshToken, "CLIENT_ID", "CLIENT_SECRET", "SCOPE"));
    }

    @Test
    void fetchAccessToken_ShouldThrowExceptionWhenAzureCallIsNotSuccessful() {
        // given
        HTTPResponse failedResponse = mockFailedResponse();
        when(oAuthClientImpl.fetchAccessToken(any(), any(), any(), any(), any())).thenReturn(failedResponse);

        // when
        HTTPResponse result = oAuthClientImpl.fetchAccessToken(providerConfigurationProperties, "REFRESH_TOKEN", "CLIENT_ID", "CLIENT_SECRET", "SCOPE");

        // then
        assertEquals(HttpStatus.SC_BAD_REQUEST, result.getStatusCode());
        assertEquals("body", result.getContent());
    }

    private HTTPResponse mockSuccessResponse() {
        HTTPResponse response = org.mockito.Mockito.mock(HTTPResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getContent()).thenReturn("{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}");
        return response;
    }

    private HTTPResponse mockFailedResponse() {
        HTTPResponse response = org.mockito.Mockito.mock(HTTPResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(response.getContent()).thenReturn("body");
        return response;
    }
}