package uk.gov.hmcts.darts.authentication.controller.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
