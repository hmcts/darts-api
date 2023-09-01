package uk.gov.hmcts.darts.authentication.controller.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;

@Slf4j
@RestController
@RequestMapping("/internal-user")
public class AuthenticationInternalUserController extends AbstractUserController {
    public AuthenticationInternalUserController(AuthenticationService authenticationService, AuthorisationApi authorisationApi) {
        super(authenticationService, authorisationApi);
    }
}
