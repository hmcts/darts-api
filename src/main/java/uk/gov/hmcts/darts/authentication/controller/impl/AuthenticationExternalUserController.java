package uk.gov.hmcts.darts.authentication.controller.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/external-user")
public class AuthenticationExternalUserController extends AbstractUserController {
    public AuthenticationExternalUserController(AuthenticationService authenticationService, AuthorisationApi authorisationApi) {
        super(authenticationService, authorisationApi);
    }
}
