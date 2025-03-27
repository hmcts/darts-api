package uk.gov.hmcts.darts.authentication.dao.impl;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.client.OAuthClient;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureDaoImplTest {

    @Mock
    private AuthConfigurationProperties authenticationConfiguration;

    @Mock
    private AuthProviderConfigurationProperties authenticationProviderConfiguration;

    @Mock
    private OAuthClient azureActiveDirectoryB2CClient;

    @InjectMocks
    private AzureDaoImpl azureDaoImpl;


    @Test
    void fetchAccessToken_ShouldReturnResponse_WhenAzureCallIsSuccessful_WithRedirectUrl() throws AzureDaoException {
        HTTPResponse response = mockSuccessResponse();
        when(azureActiveDirectoryB2CClient.fetchAccessToken(any(), any(), any(), any(), any(), any()))
            .thenReturn(response);

        OAuthProviderRawResponse rawResponse = azureDaoImpl.fetchAccessToken(
            "CODE",
            authenticationProviderConfiguration,
            authenticationConfiguration,
            null
        );

        assertEquals(
            "test_id_token",
            Objects.nonNull(rawResponse.getIdToken()) ? rawResponse.getIdToken() : rawResponse.getAccessToken()
        );
        assertEquals(
            1234L, rawResponse.getIdTokenExpiresIn());
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void fetchAccessToken_ShouldThrowException_WhenProvidedCodeIsBlankOrNull_WithRedirectUrl(String code) {
        AzureDaoException exception = assertThrows(AzureDaoException.class, () -> azureDaoImpl.fetchAccessToken(
            code, authenticationProviderConfiguration, authenticationConfiguration, null));

        assertEquals("Null code not permitted", exception.getMessage());
    }

    @Test
    void fetchAccessToken_ShouldThrowException_WhenAzureCallIsNotSuccessful_WithRedirectUrl() {
        HTTPResponse failedResponse = mockFailedResponse();
        when(azureActiveDirectoryB2CClient.fetchAccessToken(any(), any(), any(), any(), any(), any())).thenReturn(failedResponse);

        AzureDaoException exception = assertThrows(
            AzureDaoException.class,
            () -> azureDaoImpl.fetchAccessToken("CODE", authenticationProviderConfiguration, authenticationConfiguration, null)
        );

        assertEquals("Unexpected HTTP response code received from Azure: body", exception.getMessage());
        assertEquals(422, exception.getHttpStatus());
    }

    @Test
    void fetchAccessToken_ShouldReturnResponse_WhenAzureCallIsSuccessful_WithRefreshToken() throws AzureDaoException {
        // given
        HTTPResponse response = mockSuccessResponse();
        when(authenticationConfiguration.getClientId()).thenReturn("CLIENT_ID");
        when(authenticationConfiguration.getClientSecret()).thenReturn("CLIENT_SECRET");
        when(authenticationConfiguration.getScope()).thenReturn("SCOPE");
        when(azureActiveDirectoryB2CClient.fetchAccessToken(
            any(AuthProviderConfigurationProperties.class), anyString(), anyString(), anyString(), anyString())).thenReturn(response);

        // when
        OAuthProviderRawResponse rawResponse = azureDaoImpl.fetchAccessToken("REFRESH_TOKEN", authenticationProviderConfiguration, authenticationConfiguration);

        // then
        assertEquals("test_id_token", rawResponse.getIdToken());
        assertEquals(1234L, rawResponse.getIdTokenExpiresIn());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void fetchAccessToken_ShouldThrowException_WhenRefreshTokenIsBlankOrNull_WithRefreshToken(String refreshToken) {
        AzureDaoException exception = assertThrows(
            AzureDaoException.class, () -> azureDaoImpl.fetchAccessToken(refreshToken, authenticationProviderConfiguration,
                                                                         authenticationConfiguration));

        assertEquals("Null refresh token not permitted", exception.getMessage());
    }

    @Test
    void fetchAccessToken_ShouldThrowException_WhenAzureCallIsNotSuccessful_WithRefreshToken() {
        HTTPResponse failedResponse = mockFailedResponse();
        when(authenticationConfiguration.getClientId()).thenReturn("CLIENT_ID");
        when(authenticationConfiguration.getClientSecret()).thenReturn("CLIENT_SECRET");
        when(authenticationConfiguration.getScope()).thenReturn("SCOPE");
        when(azureActiveDirectoryB2CClient.fetchAccessToken(
            any(AuthProviderConfigurationProperties.class), anyString(), anyString(), anyString(), anyString())).thenReturn(failedResponse);

        AzureDaoException exception = assertThrows(AzureDaoException.class,
                                                   () -> azureDaoImpl.fetchAccessToken("REFRESH_TOKEN", authenticationProviderConfiguration,
                                                                                       authenticationConfiguration));

        assertEquals("Unexpected HTTP response code received from Azure: body", exception.getMessage());
        assertEquals(422, exception.getHttpStatus());
    }

    private HTTPResponse mockSuccessResponse() {
        String body = "{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}";

        HTTPResponse response = Mockito.mock(HTTPResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getContent()).thenReturn(body);

        return response;
    }

    private HTTPResponse mockFailedResponse() {

        HTTPResponse response = Mockito.mock(HTTPResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(response.getContent()).thenReturn("body");

        return response;
    }
}
