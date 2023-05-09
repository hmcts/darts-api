package uk.gov.hmcts.darts.authentication.controller.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationExternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LANDING_PAGE_URI = URI.create("/");

    @InjectMocks
    private AuthenticationExternalUserController controller;

    @Mock
    private AuthenticationService authenticationService;

    @Test
    void loginAndRefreshTokenFromAzureWhenTokenDoesntExistsInSession() {
        MockHttpSession session = new MockHttpSession();

        when(authenticationService.loginOrRefresh(anyString()))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.loginOrRefresh(session);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName(),
                     "Redirect url was not as expected");
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        MockHttpSession session = new MockHttpSession();

        when(authenticationService.handleOauthCode(any(), anyString()))
            .thenReturn(DUMMY_LANDING_PAGE_URI);

        ModelAndView modelAndView = controller.handleOauthCode(session, "code");

        assertNotNull(modelAndView);
        assertEquals("redirect:/", modelAndView.getViewName(),
                     "Redirect url was not as expected");
    }

    @Test
    void logoutWhenUserLogoutFromdarts() {
        assertThrows(NotImplementedException.class, () -> controller.logout());
    }

}
