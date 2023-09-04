package uk.gov.hmcts.darts.authentication.component.impl;

import groovy.transform.Sealed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthConfigurationPropertiesStrategyTest {

    @Mock
    private AuthConfigurationProperties authConfiguration;

    @Mock
    private AuthProviderConfigurationProperties provider;

    @InjectMocks
    private DummyAuthStrategy authConfig;

    @Test
    void getLoginUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();
        when(provider.getAuthorizationURI()).thenReturn("AuthUrl");

        when(authConfiguration.getResponseMode()).thenReturn("ResponseMode");
        when(authConfiguration.getResponseType()).thenReturn("ResponseType");

        URI authUrl = authConfig.getLoginUri(null);

        assertEquals(
            "AuthUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                "&response_mode=ResponseMode&response_type=ResponseType",
            authUrl.toString()
        );
    }

    @Test
    void getLoginUriShouldReturnExpectedUriWithOverriddenRedirectUri() {

        commonMocksForAuthorisation();

        when(provider.getAuthorizationURI()).thenReturn("AuthUrl");
        when(authConfiguration.getResponseMode()).thenReturn("ResponseMode");
        when(authConfiguration.getResponseType()).thenReturn("ResponseType");

        URI authUrl = authConfig.getLoginUri("OverriddenRedirectUri");

        assertEquals(
            "AuthUrl?client_id=ClientId&redirect_uri=OverriddenRedirectUri&scope=Scope&prompt=Prompt" +
                "&response_mode=ResponseMode&response_type=ResponseType",
            authUrl.toString()
        );
    }

    @Test
    void getLandingPageUriShouldReturnExpectedUri() {
        URI landingPageUri = authConfig.getLandingPageUri();

        assertEquals("/", landingPageUri.toString());
    }

    @Test
    void getLogoutUriShouldReturnExpectedUri() {

        when(provider.getLogoutURI()).thenReturn("LogoutUrl");
        when(authConfiguration.getLogoutRedirectURI()).thenReturn("LogoutRedirectUrl");

        URI logoutUri = authConfig.getLogoutUri("DUMMY_SESSION_ID", null);

        assertEquals(
            "LogoutUrl?id_token_hint=DUMMY_SESSION_ID&post_logout_redirect_uri=LogoutRedirectUrl",
            logoutUri.toString()
        );
    }

    @Test
    void getLogoutUriShouldReturnExpectedUriWithOverriddenRedirectUri() {
        when(authConfiguration.getLogoutRedirectURI()).thenReturn("LogoutRedirectUrl");
        when(provider.getLogoutURI()).thenReturn("LogoutUrl");

        URI logoutUri = authConfig.getLogoutUri("DUMMY_SESSION_ID", "OverriddenRedirectUri");

        assertEquals(
            "LogoutUrl?id_token_hint=DUMMY_SESSION_ID&post_logout_redirect_uri=OverriddenRedirectUri",
            logoutUri.toString()
        );
    }

    @Test
    void getResetPasswordUriShouldReturnExpectedUri() {
        commonMocksForAuthorisation();

        when(provider.getResetPasswordURI()).thenReturn("ResetUrl");

        URI logoutUri = authConfig.getResetPasswordUri(null);

        assertEquals(
            "ResetUrl?client_id=ClientId&redirect_uri=RedirectId&scope=Scope&prompt=Prompt" +
                "&response_type=id_token",
            logoutUri.toString()
        );
    }

    @Test
    void getResetPasswordUriShouldReturnExpectedUriWithOverriddenRedirectUri() {
        commonMocksForAuthorisation();

        when(provider.getResetPasswordURI()).thenReturn("ResetUrl");

        URI logoutUri = authConfig.getResetPasswordUri("OverriddenRedirectUri");

        assertEquals(
            "ResetUrl?client_id=ClientId&redirect_uri=OverriddenRedirectUri&scope=Scope&prompt=Prompt" +
                "&response_type=id_token",
            logoutUri.toString()
        );
    }

    private AuthConfigurationProperties commonMocksForAuthorisation() {
        when(authConfiguration.getClientId()).thenReturn("ClientId");
        when(authConfiguration.getRedirectURI()).thenReturn("RedirectId");
        when(authConfiguration.getScope()).thenReturn("Scope");
        when(authConfiguration.getPrompt()).thenReturn("Prompt");

        return authConfiguration;
    }
}

@Getter
@Setter
@RequiredArgsConstructor
class DummyAuthStrategy implements AuthenticationConfigurationPropertiesStrategy {
    private final AuthConfigurationProperties configurationProperties;
    private final AuthProviderConfigurationProperties configurationProviderProperties;

    @Override
    public AuthConfigurationProperties getConfiguration() {
        return configurationProperties;
    }

    @Override
    public AuthProviderConfigurationProperties getProviderConfiguration() {
        return configurationProviderProperties;
    }

    @Override
    public boolean doesMatch(HttpServletRequest req) {
        return false;
    }
}
