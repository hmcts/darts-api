package uk.gov.hmcts.darts.authentication.controller.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthenticationInternalUserControllerTest {

    @InjectMocks
    private AuthenticationInternalUserController controller;

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistsInSession() {
        assertThrows(NotImplementedException.class, () -> controller.handleOauthCode(null));
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        assertThrows(NotImplementedException.class, () -> controller.handleOauthCode(null));
    }

    @Test
    void logoutWhenUserLogoutFromdarts() {
        assertThrows(NotImplementedException.class, () -> controller.logout());
    }

}
