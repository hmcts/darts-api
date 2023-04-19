package uk.gov.hmcts.darts.authentication.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationExternalUserControllerTest {


    private final AuthenticationExternalUserController authExternalUserController =
        new AuthenticationExternalUserController();

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistsInSession() {
        ModelAndView mv = authExternalUserController.loginOrRefresh();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        ModelAndView mv = authExternalUserController.handleOauthCode("code", "state");
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void logoutWhenUserLogoutFromdarts() {
        ModelAndView mv = authExternalUserController.logout();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }
}
