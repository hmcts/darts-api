package uk.gov.hmcts.darts.authentication.dao.impl;

import feign.Request;
import feign.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AzureActiveDirectoryB2CClient;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureDaoImplTest {

    @Mock
    private AzureActiveDirectoryB2CClient azureActiveDirectoryB2CClient;

    @Mock
    private AuthConfiguration<?> authenticationConfiguration;

    @InjectMocks
    private AzureDaoImpl azureDaoImpl;


    @Test
    void fetchAccessTokenShouldReturnResponseWhenAzureCallIsSuccessful() throws AzureDaoException {
        mockConfig();
        try (Response response = mockSuccessResponse()) {
            when(azureActiveDirectoryB2CClient.fetchAccessToken(any())).thenReturn(response);

            OAuthProviderRawResponse rawResponse = azureDaoImpl.fetchAccessToken("CODE", authenticationConfiguration);

            assertEquals("test_id_token", rawResponse.getAccessToken());
            assertEquals(1234L, rawResponse.getExpiresIn());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void fetchAccessTokenShouldThrowExceptionWhenProvidedCodeIsBlankOrNull(String code) {
        AzureDaoException exception = assertThrows(AzureDaoException.class, () -> azureDaoImpl.fetchAccessToken(code, authenticationConfiguration));

        assertEquals("Null code not permitted", exception.getMessage());
    }

    @Test
    void fetchAccessTokenShouldThrowExceptionWhenAzureCallIsNotSuccessful() {
        mockConfig();
        try (Response response = mockSuccessResponse()) {
            try (Response failedResponse = mockFailedResponse(response)) {
                when(azureActiveDirectoryB2CClient.fetchAccessToken(any())).thenReturn(failedResponse);

                AzureDaoException exception = assertThrows(
                    AzureDaoException.class,
                    () -> azureDaoImpl.fetchAccessToken("CODE", authenticationConfiguration)
                );

                assertEquals("Unexpected HTTP response code received from Azure: body", exception.getMessage());
                assertEquals(400, exception.getHttpStatus());
            }
        }
    }

    private Response mockSuccessResponse() {
        String body = "{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}";
        Map<String, Collection<String>> headersError = new ConcurrentHashMap<>();

        return Response.builder().reason("REASON").body(body, StandardCharsets.UTF_8).status(HttpStatus.SC_OK)
            .headers(new HashMap<>())
            .request(Request.create(Request.HttpMethod.POST, "dummy/test", headersError, null, null, null)).build();
    }

    private Response mockFailedResponse(Response response) {
        return response.toBuilder().status(HttpStatus.SC_BAD_REQUEST).body("body", StandardCharsets.UTF_8).build();
    }

    private void mockConfig() {
        when(authenticationConfiguration.getClientId()).thenReturn("ClientId");
        when(authenticationConfiguration.getRedirectURI()).thenReturn("RedirectId");
        when(authenticationConfiguration.getScope()).thenReturn("Scope");
        when(authenticationConfiguration.getGrantType()).thenReturn("GrantType");
        when(authenticationConfiguration.getSecret()).thenReturn("ClientSecret");
    }

}
