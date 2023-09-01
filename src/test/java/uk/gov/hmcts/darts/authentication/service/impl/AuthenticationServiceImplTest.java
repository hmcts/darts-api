package uk.gov.hmcts.darts.authentication.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationLocator;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final URI DUMMY_AUTH_URI = URI.create("DUMMY_AUTH_URI");
    private static final URI DUMMY_LOGOUT_URI = URI.create("DUMMY_LOGOUT_URI");
    private static final URI DUMMY_LANDING_PAGE_URI = URI.create("DUMMY_LANDING_PAGE_URI");
    private static final String DUMMY_CODE = "DUMMY CODE";
    private static final String DUMMY_ID_TOKEN = "DUMMY ID TOKEN";

    @InjectMocks
    private AuthenticationServiceimpl authenticationService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private AzureDao azureDao;

    @Mock
    private AuthConfigurationLocator uriProvider;

    @Test
    @SuppressWarnings("unchecked")
    void loginOrRefreshShouldReturnAuthUriWhenNoAuthHeaderExists() {

        AuthConfiguration authConfigMock = Mockito.mock(AuthConfiguration.class);
        when(uriProvider.locateAuthenticationConfigurationWithExternalDefault()).thenReturn(authConfigMock);
        when(authConfigMock.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.loginOrRefresh(null, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginOrRefreshShouldReturnAuthUriWhenInvalidAccessTokenExists() {

        AuthConfiguration authConfigMock = Mockito.mock(AuthConfiguration.class);
        when(uriProvider.locateAuthenticationConfigurationWithExternalDefault()).thenReturn(authConfigMock);

        when(authConfigMock.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN, authConfigMock))
            .thenReturn(new JwtValidationResult(false, "Invalid token"));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginOrRefreshShouldReturnLandingPageUriWhenValidAccessTokenExists() {

        AuthConfiguration authConfigMock = Mockito.mock(AuthConfiguration.class);
        when(uriProvider.locateAuthenticationConfigurationWithExternalDefault()).thenReturn(authConfigMock);

        when(authConfigMock.getLandingPageUri())
            .thenReturn(DUMMY_LANDING_PAGE_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN, authConfigMock))
            .thenReturn(new JwtValidationResult(true, null));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LANDING_PAGE_URI, uri);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleOauthCodeShouldReturnLandingPageUriWhenTokenIsObtainedAndValid() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString(), notNull()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString(), notNull()))
            .thenReturn(new JwtValidationResult(true, null));

        String token = authenticationService.handleOauthCode(DUMMY_CODE);

        assertEquals(DUMMY_ID_TOKEN, token);
    }

    @Test
    @SuppressWarnings("")
    void handleOauthCodeShouldThrowExceptionWhenFetchAccessTokenThrowsException() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString(), notNull()))
            .thenThrow(AzureDaoException.class);

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE)
        );

        assertEquals("100", exception.getError().getErrorTypeNumeric());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleOauthCodeShouldThrowExceptionWhenValidationFails() throws AzureDaoException {

        when(azureDao.fetchAccessToken(anyString(), notNull()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString(), notNull()))
            .thenReturn(new JwtValidationResult(false, "validation failure reason"));

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE)
        );

        assertEquals("101", exception.getError().getErrorTypeNumeric());
    }

    @Test
    @SuppressWarnings("unchecked")
    void logoutShouldReturnLogoutPageUriWhenSessionExists() {

        AuthConfiguration authConfigMock = Mockito.mock(AuthConfiguration.class);
        when(uriProvider.locateAuthenticationConfigurationWithExternalDefault()).thenReturn(authConfigMock);

        when(authConfigMock.getLogoutUri(anyString(), any()))
            .thenReturn(DUMMY_LOGOUT_URI);

        URI uri = authenticationService.logout(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LOGOUT_URI, uri);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetPasswordShouldReturnResetPasswordUri() {


        AuthConfiguration authConfigMock = Mockito.mock(AuthConfiguration.class);
        when(uriProvider.locateAuthenticationConfigurationWithExternalDefault()).thenReturn(authConfigMock);

        when(authConfigMock.getResetPasswordUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.resetPassword(null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

}
