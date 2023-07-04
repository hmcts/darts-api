package uk.gov.hmcts.darts.authentication.component.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UriProviderImplTest {

    @Mock
    private AuthenticationConfiguration authConfig;

    @InjectMocks
    private UriProviderImpl uriProvider;

    @Test
    void getLoginUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();
        when(authConfig.getExternalADauthorizationUri()).thenReturn("AuthUrl");
        when(authConfig.getExternalADresponseMode()).thenReturn("ResponseMode");
        when(authConfig.getExternalADresponseType()).thenReturn("ResponseType");

        URI authUrl = uriProvider.getLoginUri();

        assertEquals("AuthUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                         "&response_mode=ResponseMode&response_type=ResponseType",
                     authUrl.toString());
    }

    @Test
    void getLandingPageUriShouldReturnExpectedUri() {
        URI landingPageUri = uriProvider.getLandingPageUri();

        assertEquals("/", landingPageUri.toString());
    }

    @Test
    void getLogoutUriShouldReturnExpectedUri() {
        when(authConfig.getExternalADlogoutUri()).thenReturn("LogoutUrl");
        when(authConfig.getExternalADlogoutRedirectUri()).thenReturn("LogoutRedirectUrl");

        URI logoutUri = uriProvider.getLogoutUri("DUMMY_SESSION_ID");

        assertEquals("LogoutUrl?id_token_hint=DUMMY_SESSION_ID&post_logout_redirect_uri=LogoutRedirectUrl",
                     logoutUri.toString());
    }

    @Test
    void getResetPasswordUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();
        when(authConfig.getExternalADresetPasswordUri()).thenReturn("ResetUrl");

        URI logoutUri = uriProvider.getResetPasswordUri();

        assertEquals("ResetUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                         "&response_type=id_token",
                     logoutUri.toString());
    }

    private void commonMocksForAuthorisation() {
        when(authConfig.getExternalADclientId()).thenReturn("ClientId");
        when(authConfig.getExternalADredirectUri()).thenReturn("RedirectId");
        when(authConfig.getExternalADscope()).thenReturn("Scope");
        when(authConfig.getExternalADprompt()).thenReturn("Prompt");
    }

}
