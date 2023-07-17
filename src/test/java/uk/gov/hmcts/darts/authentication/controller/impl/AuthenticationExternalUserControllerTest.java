package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logout?param=value");
    private static final String DUMMY_TOKEN = "token";

    @InjectMocks
    private AuthenticationExternalUserController controller;

    @Mock
    private AuthenticationService authenticationService;

    @Test
    void loginAndRefreshShouldReturnLoginPageAsRedirectWhenAuthHeaderIsNotSet() {
        when(authenticationService.loginOrRefresh(null))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.loginOrRefresh(null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() {
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(DUMMY_TOKEN);

        String accessToken = controller.handleOauthCode("code");

        assertNotNull(accessToken);
    }

    @Test
    void logoutShouldReturnLogoutPageUriAsRedirectWhenTokenExistsInSession() {
        when(authenticationService.logout(any()))
            .thenReturn(DUMMY_LOGOUT_URI);

        ModelAndView modelAndView = controller.logout(anyString());

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/logout?param=value", modelAndView.getViewName());
    }

    @Test
    void resetPasswordShouldReturnResetPageAsRedirect() {
        when(authenticationService.resetPassword())
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.resetPassword();

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

}
