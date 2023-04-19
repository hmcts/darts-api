package uk.gov.hmcts.darts.authentication.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationInternalUserControllerTest {

    private final AuthenticationInternalUserController authInternalUserController =
        new AuthenticationInternalUserController();

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistsInSession() {
        ModelAndView mv = authInternalUserController.loginOrRefresh();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        ModelAndView mv = authInternalUserController.handleOauthCode("code", "state");
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void logoutWhenUserLogoutFromdarts() {
        ModelAndView mv = authInternalUserController.logout();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

}
