package uk.gov.hmcts.reform.darts.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.reform.darts.controllers.authentication.AuthInternalUserController;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInternalUserControllerTest {

    private final AuthInternalUserController authInternalUserController = new AuthInternalUserController();

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistInSession() {
        ModelAndView mv = authInternalUserController.loginOrRefresh();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void handleOuthcodefromazureWhencodeisreturned() {
        ModelAndView mv = authInternalUserController.handleOauthCode("code", "state");
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }

    @Test
    void logoutWhenuserlogoutfromdarts() {
        ModelAndView mv = authInternalUserController.logout();
        assertTrue(Objects.nonNull(mv), "The Model View is not null");
    }
}
