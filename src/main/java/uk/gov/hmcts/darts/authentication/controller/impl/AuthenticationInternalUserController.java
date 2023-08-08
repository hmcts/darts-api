package uk.gov.hmcts.darts.authentication.controller.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;

@Slf4j
@RestController
@RequestMapping("/internal-user")
public class AuthenticationInternalUserController implements AuthenticationController {

    private static final String INTERNAL_USERS_NOT_SUPPORTED_MESSAGE = "Internal users not yet supported";

    @Override
    public ModelAndView loginOrRefresh(String authHeaderValue) {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public SecurityToken handleOauthCode(String code) {
        log.info("Authorization Token received successfully");
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public ModelAndView logout(String authHeaderValue) {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public ModelAndView resetPassword() {
        throw new NotImplementedException(INTERNAL_USERS_NOT_SUPPORTED_MESSAGE);
    }

}
