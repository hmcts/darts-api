package uk.gov.hmcts.reform.darts.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.reform.darts.controllers.authentication.AuthExternalUserController;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;


class AuthExternalUserControllerTest {


    private final AuthExternalUserController authExternalUserController = new AuthExternalUserController();

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
