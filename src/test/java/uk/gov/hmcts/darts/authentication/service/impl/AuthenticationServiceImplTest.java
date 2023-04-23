package uk.gov.hmcts.darts.authentication.service.impl;

import feign.Request;
import feign.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AzureActiveDirectoryB2CClient;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AzureActiveDirectoryB2CClient azureActiveDirectoryB2CClient;

    private static final String AUTHORIZE_URL = "AuthUrl?client_id=ClientId&response_type=ResponseType&redirect_uri="
        + "RedirectId&response_mode=ResponseMode&scope=Scope&prompt=Prompt";

    @Test
    void testGetAuthorizationUrlFromConfigWhenLoginRequested() {
        mockStubsForAutorizationCode();
        String authUrl = authenticationService.getAuthorizationUrl();
        assertEquals(AUTHORIZE_URL, authUrl, "Expected Authorize URL is generated");
    }

    @Test
    void fetchAccessTokenSuccesfulWhenAuthorizationCodeIsPassed() {
        mockStubsForAccessToken();
        try (Response response = mockResponse()) {
            when(azureActiveDirectoryB2CClient.fetchAccessToken(any())).thenReturn(response);
            OAuthProviderRawResponse rawResponse = authenticationService.fetchAccessToken("CODE");
            assertEquals("test_id_token", rawResponse.getAccessToken(), "Valid Access Token is returned by Azure");
            assertEquals(1234L, rawResponse.getExpiresIn(), "Valid Expiry Time is returned by Azure");
        }
    }

    @Test
    void fetchAccessTokenUnsuccesfulWhenAuthorizationCodeIsEmpty() {
        mockStubsForAccessToken();
        try (Response response = mockResponse()) {
            try (Response failedResponse = mockFailedResponse(response)) {
                when(azureActiveDirectoryB2CClient.fetchAccessToken(any())).thenReturn(failedResponse);
                OAuthProviderRawResponse rawResponse = authenticationService.fetchAccessToken(null);
                assertNull(
                    rawResponse.getAccessToken(),
                    "No Access Token is returned by Azure for Invalid Request"
                );
                assertEquals(0, rawResponse.getExpiresIn(), "No Expiry Time is returned by Azure for Invalid Request");
            }
        }
    }

    private Response mockResponse() {
        String body = "{\"id_token\":\"test_id_token\", \"id_token_expires_in\":\"1234\"}";
        Map<String, Collection<String>> headersError = new ConcurrentHashMap<>();

        return Response.builder()
            .reason("REASON")
            .body(body, StandardCharsets.UTF_8)
            .status(HttpStatus.SC_OK)
            .headers(new HashMap<>())
            .request(Request.create(
                Request.HttpMethod.POST,
                "dummy/test",
                headersError,
                null,
                null,
                null
            ))
            .build();
    }

    private Response mockFailedResponse(Response response) {
        return response.toBuilder().status(HttpStatus.SC_BAD_REQUEST).body(
            "body",
            StandardCharsets.UTF_8
        ).build();
    }

    private void mockStubsForAutorizationCode() {
        mockCommonStubs();
        when(authenticationConfiguration.getExternalADauthorizationUri()).thenReturn("AuthUrl");
        when(authenticationConfiguration.getExternalADredirectUri()).thenReturn("RedirectId");
        when(authenticationConfiguration.getExternalADresponseMode()).thenReturn("ResponseMode");
        when(authenticationConfiguration.getExternalADresponseType()).thenReturn("ResponseType");
        when(authenticationConfiguration.getExternalADprompt()).thenReturn("Prompt");
    }

    private void mockStubsForAccessToken() {
        mockCommonStubs();
        when(authenticationConfiguration.getExternalADauthorizationGrantType()).thenReturn("GrantType");
        when(authenticationConfiguration.getExternalADclientSecret()).thenReturn("ClientSecret");
    }

    private void mockCommonStubs() {
        when(authenticationConfiguration.getExternalADclientId()).thenReturn("ClientId");
        when(authenticationConfiguration.getExternalADredirectUri()).thenReturn("RedirectId");
        when(authenticationConfiguration.getExternalADscope()).thenReturn("Scope");
    }
}
