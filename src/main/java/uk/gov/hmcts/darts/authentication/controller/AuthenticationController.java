package uk.gov.hmcts.darts.authentication.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authentication.service.SessionService;

@Slf4j
@Component
public class AuthenticationController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/handle-oauth-code")
    public ModelAndView handleOauthCode(String code) {
        log.info("Azure AD Token received successfully");
        authenticationService.fetchAccessToken(code);
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/logout")
    public ModelAndView logout() {
        return new ModelAndView("redirect:/external-user/login-or-refresh");
    }

    public String getAuthorizeUrl() {
        return authenticationService.getAuthorizationUrl();
    }
}
