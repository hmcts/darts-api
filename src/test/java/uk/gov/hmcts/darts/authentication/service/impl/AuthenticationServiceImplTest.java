package uk.gov.hmcts.darts.authentication.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.model.TokenResponse;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final URI DUMMY_AUTH_URI = URI.create("DUMMY_AUTH_URI");
    private static final URI DUMMY_LOGOUT_URI = URI.create("DUMMY_LOGOUT_URI");
    private static final URI DUMMY_LANDING_PAGE_URI = URI.create("DUMMY_LANDING_PAGE_URI");
    private static final String DUMMY_CODE = "DUMMY CODE";
    private static final String DUMMY_ID_TOKEN = "DUMMY ID TOKEN";
    private static final String DUMMY_REFRESH_TOKEN = "DUMMY REFRESH TOKEN";

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private AzureDao azureDao;

    @Mock
    private AuthStrategySelector uriProvider;

    @Mock
    private ExternalAuthConfigurationProperties externalAuthConfigurationProperties;

    @Test
    void loginOrRefreshShouldReturnAuthUriWhenNoAuthHeaderExists() {

        AuthenticationConfigurationPropertiesStrategy authStrategyMock = Mockito.mock(AuthenticationConfigurationPropertiesStrategy.class);
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(authStrategyMock);

        when(authStrategyMock.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.loginOrRefresh(null, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    void loginOrRefreshShouldReturnAuthUriWhenInvalidAccessTokenExists() {

        AuthenticationConfigurationPropertiesStrategy authStrategyMock = Mockito.mock(AuthenticationConfigurationPropertiesStrategy.class);
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(authStrategyMock);

        when(authStrategyMock.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN, authStrategyMock.getProviderConfiguration(), authStrategyMock.getConfiguration()))
            .thenReturn(new JwtValidationResult(false, "Invalid token"));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    void loginOrRefreshShouldReturnLandingPageUriWhenValidAccessTokenExists() {

        AuthenticationConfigurationPropertiesStrategy authStrategyMock = Mockito.mock(AuthenticationConfigurationPropertiesStrategy.class);
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(authStrategyMock);

        when(authStrategyMock.getLandingPageUri())
            .thenReturn(DUMMY_LANDING_PAGE_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN, authStrategyMock.getProviderConfiguration(), authStrategyMock.getConfiguration()))
            .thenReturn(new JwtValidationResult(true, null));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LANDING_PAGE_URI, uri);
    }

    @Test
    void handleOauthCodeShouldReturnLandingPageUriWhenTokenIsObtainedAndValid() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString(), notNull(), notNull(), isNull()))
            .thenReturn(new OAuthProviderRawResponse(null, 0, DUMMY_ID_TOKEN, 0, DUMMY_REFRESH_TOKEN));
        when(tokenValidator.validate(anyString(), notNull(), notNull()))
            .thenReturn(new JwtValidationResult(true, null));
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(
            new ExternalAuthConfigurationPropertiesStrategy(externalAuthConfigurationProperties,
                                                            new ExternalAuthProviderConfigurationProperties()));

        TokenResponse token = authenticationService.handleOauthCode(DUMMY_CODE, null);

        assertEquals(DUMMY_ID_TOKEN, token.accessToken());
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenFetchAccessTokenThrowsException() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString(), notNull(), notNull(), isNull()))
            .thenThrow(AzureDaoException.class);

        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE, null)
        );

        assertEquals("AUTHENTICATION_100", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenValidationFails() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString(), notNull(), notNull(), isNull()))
            .thenReturn(new OAuthProviderRawResponse(null, 0, DUMMY_ID_TOKEN, 0, DUMMY_REFRESH_TOKEN));
        when(tokenValidator.validate(anyString(), notNull(), notNull()))
            .thenReturn(new JwtValidationResult(false, "validation failure reason"));
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(
            new ExternalAuthConfigurationPropertiesStrategy(
                externalAuthConfigurationProperties,
                new ExternalAuthProviderConfigurationProperties()
            ));

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE, null)
        );

        assertEquals("AUTHENTICATION_101", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void logoutShouldReturnLogoutPageUriWhenSessionExists() {

        AuthenticationConfigurationPropertiesStrategy authStrategyMock = Mockito.mock(AuthenticationConfigurationPropertiesStrategy.class);

        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(authStrategyMock);

        when(authStrategyMock.getLogoutUri(anyString(), any()))
            .thenReturn(DUMMY_LOGOUT_URI);

        URI uri = authenticationService.logout(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LOGOUT_URI, uri);
    }

    @Test
    void resetPasswordShouldReturnResetPasswordUri() {


        AuthenticationConfigurationPropertiesStrategy authStrategyMock = Mockito.mock(AuthenticationConfigurationPropertiesStrategy.class);
        when(uriProvider.locateAuthenticationConfiguration()).thenReturn(authStrategyMock);

        when(authStrategyMock.getResetPasswordUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.resetPassword(null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }
}
