package uk.gov.hmcts.darts.authentication.component.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthConfigurationTest {

    @Mock
    private AuthConfiguration<?> authConfig;

    @Test
    void getLoginUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();
        when(authConfig.getProvider().getAuthorizationURI()).thenReturn("AuthUrl");
        when(authConfig.getResponseMode()).thenReturn("ResponseMode");
        when(authConfig.getResponseType()).thenReturn("ResponseType");

        URI authUrl = authConfig.getLoginUri(null);

        assertEquals("AuthUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                         "&response_mode=ResponseMode&response_type=ResponseType",
                     authUrl.toString());
    }

    @Test
    void getLoginUriShouldReturnExpectedUriWithOverriddenRedirectUri() {
        commonMocksForAuthorisation();
        when(authConfig.getProvider().getAuthorizationURI()).thenReturn("AuthUrl");
        when(authConfig.getResponseMode()).thenReturn("ResponseMode");
        when(authConfig.getResponseType()).thenReturn("ResponseType");

        URI authUrl = authConfig.getLoginUri("OverriddenRedirectUri");

        assertEquals("AuthUrl?client_id=ClientId&redirect_uri=OverriddenRedirectUri&scope=Scope&prompt=Prompt" +
                         "&response_mode=ResponseMode&response_type=ResponseType",
                     authUrl.toString());
    }

    @Test
    void getLandingPageUriShouldReturnExpectedUri() {
        URI landingPageUri = authConfig.getLandingPageUri();

        assertEquals("/", landingPageUri.toString());
    }

    @Test
    void getLogoutUriShouldReturnExpectedUri() {
        when(authConfig.getProvider().getLogoutURI()).thenReturn("LogoutUrl");
        when(authConfig.getRedirectURI()).thenReturn("LogoutRedirectUrl");

        URI logoutUri = authConfig.getLogoutUri("DUMMY_SESSION_ID", null);

        assertEquals("LogoutUrl?id_token_hint=DUMMY_SESSION_ID&post_logout_redirect_uri=LogoutRedirectUrl",
                     logoutUri.toString());
    }

    @Test
    void getLogoutUriShouldReturnExpectedUriWithOverriddenRedirectUri() {
        when(authConfig.getProvider().getLogoutURI()).thenReturn("LogoutUrl");
        when(authConfig.getLogoutRedirectURI()).thenReturn("LogoutRedirectUrl");

        URI logoutUri = authConfig.getLogoutUri("DUMMY_SESSION_ID", "OverriddenRedirectUri");

        assertEquals("LogoutUrl?id_token_hint=DUMMY_SESSION_ID&post_logout_redirect_uri=OverriddenRedirectUri",
                     logoutUri.toString());
    }

    @Test
    void getResetPasswordUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();
        when(authConfig.getProvider().getResetPasswordURI()).thenReturn("ResetUrl");

        URI logoutUri = authConfig.getResetPasswordUri(null);

        assertEquals("ResetUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                         "&response_type=id_token",
                     logoutUri.toString());
    }

    @Test
    void getResetPasswordUriShouldReturnExpectedUriWithOverriddenRedirectUri() {
        commonMocksForAuthorisation();
        when(authConfig.getProvider().getResetPasswordURI()).thenReturn("ResetUrl");

        URI logoutUri = authConfig.getResetPasswordUri("OverriddenRedirectUri");

        assertEquals("ResetUrl?client_id=ClientId&redirect_uri=OverriddenRedirectUri&scope=Scope&prompt=Prompt" +
                         "&response_type=id_token",
                     logoutUri.toString());
    }

    private void commonMocksForAuthorisation() {
        when(authConfig.getClientId()).thenReturn("ClientId");
        when(authConfig.getRedirectURI()).thenReturn("RedirectId");
        when(authConfig.getScope()).thenReturn("Scope");
        when(authConfig.getPrompt()).thenReturn("Prompt");
    }

}
