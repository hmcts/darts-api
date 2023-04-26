package uk.gov.hmcts.darts.authentication.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationInternalUserControllerTest {

    @InjectMocks
    private AuthenticationInternalUserController authInternalUserController;

    @Mock
    private AuthenticationService authenticationService;

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistsInSession() {
        ModelAndView mv = authInternalUserController.loginOrRefresh();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        when(authenticationService.fetchAccessToken("code")).thenReturn(new OAuthProviderRawResponse());
        ModelAndView mv = authInternalUserController.handleOauthCode("code");
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void logoutWhenUserLogoutFromdarts() {
        ModelAndView mv = authInternalUserController.logout();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

}
