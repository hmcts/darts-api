package uk.gov.hmcts.darts.authentication.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.component.UriProvider;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.SessionService;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final String DUMMY_SESSION_ID = "9D65049E1787A924E269747222F60CAA";
    private static final URI DUMMY_AUTH_URI = URI.create("DUMMY_AUTH_URI");
    private static final URI DUMMY_LOGOUT_URI = URI.create("DUMMY_LOGOUT_URI");
    private static final URI DUMMY_LANDING_PAGE_URI = URI.create("DUMMY_LANDING_PAGE_URI");
    private static final String DUMMY_CODE = "DUMMY CODE";
    private static final String DUMMY_ID_TOKEN = "DUMMY ID TOKEN";

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private SessionService sessionService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private AzureDao azureDao;

    @Mock
    private UriProvider uriProvider;

    @Test
    void loginOrRefreshShouldReturnAuthUriWhenNoExistingSessionExists() {
        when(sessionService.getSession(anyString()))
            .thenReturn(null);
        when(uriProvider.getAuthorizationUri())
            .thenReturn(DUMMY_AUTH_URI);

        URI uri = authenticationService.loginOrRefresh(DUMMY_SESSION_ID);

        assertEquals(DUMMY_AUTH_URI, uri);
    }

    @Test
    void loginOrRefreshShouldReturnLandingPageUriWhenSessionExists() {
        when(sessionService.getSession(anyString()))
            .thenReturn(new Session(null, null, 0));
        when(uriProvider.getLandingPageUri())
            .thenReturn(DUMMY_LANDING_PAGE_URI);

        URI uri = authenticationService.loginOrRefresh(DUMMY_SESSION_ID);

        assertEquals(DUMMY_LANDING_PAGE_URI, uri);
    }

    @Test
    void handleOauthCodeShouldReturnLandingPageUriWhenTokenIsObtainedAndValid() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString()))
            .thenReturn(new JwtValidationResult(true, null));

        String token = authenticationService.handleOauthCode(DUMMY_SESSION_ID, DUMMY_CODE);

        assertEquals(DUMMY_ID_TOKEN, token);
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenFetchAccessTokenThrowsException() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenThrow(AzureDaoException.class);

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.handleOauthCode(DUMMY_SESSION_ID, DUMMY_CODE)
        );

        assertEquals("Failed to obtain access token", exception.getMessage());
    }

    @Test
    void handleOauthCodeShouldThrowExceptionWhenValidationFails() throws AzureDaoException {
        when(azureDao.fetchAccessToken(anyString()))
            .thenReturn(new OAuthProviderRawResponse(DUMMY_ID_TOKEN, 0));
        when(tokenValidator.validate(anyString()))
            .thenReturn(new JwtValidationResult(false, "validation failure reason"));

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.handleOauthCode(DUMMY_SESSION_ID, DUMMY_CODE)
        );

        assertEquals("Failed to validate access token: validation failure reason", exception.getMessage());
    }

    @Test
    void logoutShouldThrowExceptionWhenNoExistingSessionExists() {
        when(sessionService.getSession(anyString()))
            .thenReturn(null);

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.logout(DUMMY_SESSION_ID)
        );

        assertEquals("Session 9D65049E1787A924E269747222F60CAA attempted logout but this session is not active",
                     exception.getMessage());
    }

    @Test
    void logoutShouldReturnLogoutPageUriWhenSessionExists() {
        when(sessionService.getSession(anyString()))
            .thenReturn(new Session(null, null, 0));
        when(uriProvider.getLogoutUri(anyString()))
            .thenReturn(DUMMY_LOGOUT_URI);

        URI uri = authenticationService.logout(DUMMY_SESSION_ID);

        assertEquals(DUMMY_LOGOUT_URI, uri);
    }

    @Test
    void invalidateSessionShouldShouldCompleteWithoutExceptionWhenSessionDoesNotExist() {
        when(sessionService.dropSession(anyString()))
            .thenReturn(null);

        assertDoesNotThrow(() -> authenticationService.invalidateSession(DUMMY_SESSION_ID));
    }

    @Test
    void invalidateSessionShouldCompleteWithoutExceptionWhenSessionExists() {
        when(sessionService.dropSession(anyString()))
            .thenReturn(new Session(null, null, 0));

        assertDoesNotThrow(() -> authenticationService.invalidateSession(DUMMY_SESSION_ID));
    }

}
