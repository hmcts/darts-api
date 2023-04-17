package uk.gov.hmcts.reform.darts.controllers.authentication;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.reform.darts.config.AuthServiceConfiguration;
import uk.gov.hmcts.reform.darts.services.authentication.AuthHelperService;
import uk.gov.hmcts.reform.darts.services.authentication.AuthService;
import uk.gov.hmcts.reform.darts.services.authentication.SessionService;

@Slf4j
@Component
public class AuthController {

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    @Autowired
    private AuthServiceConfiguration authServiceConfiguration;
    @Autowired
    private AuthHelperService authHelperService;
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
