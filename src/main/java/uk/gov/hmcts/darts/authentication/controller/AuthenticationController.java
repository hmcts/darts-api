package uk.gov.hmcts.darts.authentication.controller;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;
import uk.gov.hmcts.darts.authentication.service.AuthenticationHelperService;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authentication.service.SessionService;

@Slf4j
@Component
public class AuthenticationController {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;
    @Autowired
    private AuthenticationHelperService authenticationHelperService;
    @Autowired
    private SessionService sessionService;

    @PostMapping("/handle-oauth-code")
    public ModelAndView handleOauthCode(String code, String state) {
        logger.info("Azure AD Token received successfully");
        return new ModelAndView();
    }

    @GetMapping("/logout")
    public ModelAndView logout() {
        return new ModelAndView();
    }
}
