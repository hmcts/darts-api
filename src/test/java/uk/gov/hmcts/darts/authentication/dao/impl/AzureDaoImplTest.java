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
import uk.gov.hmcts.darts.authentication.client.impl.OAuthClientImpl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void fetchAccessTokenShouldReturnResponseWhenAzureCallIsSuccessful() throws AzureDaoException {
        HTTPResponse response = mockSuccessResponse();
        when(azureActiveDirectoryB2CClient.fetchAccessToken(any(), any(), any(), any(), any())).thenReturn(response);

        OAuthProviderRawResponse rawResponse = azureDaoImpl.fetchAccessToken("CODE", authenticationProviderConfiguration, authenticationConfiguration);

        assertEquals("test_id_token", rawResponse.getAccessToken());
        assertEquals(1234L, rawResponse.getExpiresIn());
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void fetchAccessTokenShouldThrowExceptionWhenProvidedCodeIsBlankOrNull(String code) {
        AzureDaoException exception = assertThrows(AzureDaoException.class, () -> azureDaoImpl.fetchAccessToken(code, authenticationProviderConfiguration, authenticationConfiguration));

        assertEquals("Null code not permitted", exception.getMessage());
    }

    @Test
    void fetchAccessTokenShouldThrowExceptionWhenAzureCallIsNotSuccessful() {
        HTTPResponse failedResponse = mockFailedResponse();
        when(azureActiveDirectoryB2CClient.fetchAccessToken(any(), any(), any(), any(), any())).thenReturn(failedResponse);

        AzureDaoException exception = assertThrows(
            AzureDaoException.class,
            () -> azureDaoImpl.fetchAccessToken("CODE", authenticationProviderConfiguration, authenticationConfiguration)
        );

        assertEquals("Unexpected HTTP response code received from Azure: body", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
    }

    private HTTPResponse mockSuccessResponse() {
        String body = "{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}";
        Map<String, Collection<String>> headersError = new ConcurrentHashMap<>();

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

    private void mockConfig() {
        when(authenticationConfiguration.getClientId()).thenReturn("ClientId");
        when(authenticationConfiguration.getRedirectURI()).thenReturn("RedirectId");
        when(authenticationConfiguration.getScope()).thenReturn("Scope");
        when(authenticationConfiguration.getGrantType()).thenReturn("GrantType");
        when(authenticationConfiguration.getClientSecret()).thenReturn("ClientSecret");
    }

}
