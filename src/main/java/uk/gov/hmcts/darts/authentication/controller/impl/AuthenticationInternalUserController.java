package uk.gov.hmcts.darts.authentication.controller.impl;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;

@Slf4j
@RestController
@RequestMapping("/internal-user")
public class AuthenticationInternalUserController implements AuthenticationController {

    private static final String INTERNAL_USERS_NOT_SUPPORTED_MESSAGE = "Internal users not yet supported";

    @Override
    public ModelAndView loginOrRefresh(HttpSession session) {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public String handleOauthCode(HttpSession session, String code) {
        log.info("Authorization Token received successfully");

        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public ModelAndView logout(HttpSession httpSession) {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public void invalidateSession(HttpSession session) {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

}
