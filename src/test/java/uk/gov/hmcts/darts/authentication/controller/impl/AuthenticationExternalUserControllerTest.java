package uk.gov.hmcts.darts.authentication.controller.impl;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationExternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logoutpage");
    private static final String DUMMY_TOKEN = "token";

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
            .thenReturn(DUMMY_TOKEN);

        String accessToken = controller.handleOauthCode(session, "code");

        assertNotNull(accessToken);
    }

    @Test
    void logoutShouldReturnRedirectToLogoutPage() {
        when(authenticationService.logout())
            .thenReturn(DUMMY_LOGOUT_URI);

        ModelAndView modelAndView = controller.logout();

        assertEquals("redirect:https://www.example.com/logoutpage", modelAndView.getViewName());
    }

}
