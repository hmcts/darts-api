package uk.gov.hmcts.darts.authentication.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.component.UriProvider;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final URI DUMMY_AUTH_URI = URI.create("DUMMY_AUTH_URI");
    private static final URI DUMMY_LOGOUT_URI = URI.create("DUMMY_LOGOUT_URI");
    private static final URI DUMMY_LANDING_PAGE_URI = URI.create("DUMMY_LANDING_PAGE_URI");
    private static final String DUMMY_CODE = "DUMMY CODE";
    private static final String DUMMY_ID_TOKEN = "DUMMY ID TOKEN";

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private AzureDao azureDao;

    @Mock
    private UriProvider uriProvider;

    @Test
    void loginOrRefreshShouldReturnAuthUriWhenNoAuthHeaderExists() {
        when(uriProvider.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.loginOrRefresh(null, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    void loginOrRefreshShouldReturnAuthUriWhenInvalidAccessTokenExists() {
        when(uriProvider.getLoginUri(null))
            .thenReturn(DUMMY_AUTH_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN))
            .thenReturn(new JwtValidationResult(false, "Invalid token"));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    void loginOrRefreshShouldReturnLandingPageUriWhenValidAccessTokenExists() {
        when(uriProvider.getLandingPageUri())
            .thenReturn(DUMMY_LANDING_PAGE_URI);
        when(tokenValidator.validate(DUMMY_ID_TOKEN))
            .thenReturn(new JwtValidationResult(true, null));

        URI uri = authenticationService.loginOrRefresh(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LANDING_PAGE_URI, uri);
    }

    @Test
    void handleOauthCodeShouldReturnLandingPageUriWhenTokenIsObtainedAndValid() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString()))
            .thenReturn(new JwtValidationResult(true, null));

        String token = authenticationService.handleOauthCode(DUMMY_CODE);

        assertEquals(DUMMY_ID_TOKEN, token);
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenFetchAccessTokenThrowsException() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenThrow(AzureDaoException.class);

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE)
        );

        assertEquals("100", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenValidationFails() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString()))
            .thenReturn(new JwtValidationResult(false, "validation failure reason"));

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> authenticationService.handleOauthCode(DUMMY_CODE)
        );

        assertEquals("101", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void logoutShouldReturnLogoutPageUriWhenSessionExists() {
        when(uriProvider.getLogoutUri(anyString(), any()))
            .thenReturn(DUMMY_LOGOUT_URI);

        URI uri = authenticationService.logout(DUMMY_ID_TOKEN, null);

        assertEquals(DUMMY_LOGOUT_URI, uri);
    }

    @Test
    void resetPasswordShouldReturnResetPasswordUri() {
        when(uriProvider.getResetPasswordUri(null))
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.resetPassword(null);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

}
