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
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;
import java.text.ParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/external-user")
@RequiredArgsConstructor
public class AuthenticationExternalUserController implements AuthenticationController {

    private static final String EMAILS_CLAIM_NAME = "emails";

    private final AuthenticationService authenticationService;
    private final AuthorisationService authorisationService;

    @Override
    public ModelAndView loginOrRefresh(String authHeaderValue) {
        String accessToken = null;
        if (authHeaderValue != null) {
            accessToken = authHeaderValue.replace("Bearer ", "");
        }
        URI url = authenticationService.loginOrRefresh(accessToken);
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public SecurityToken handleOauthCode(String code) {
        String accessToken = authenticationService.handleOauthCode(code);
        try {
            return SecurityToken.builder()
                .accessToken(accessToken)
                .userState(authorisationService.getAuthorisation(parseEmailAddressFromAccessToken(accessToken)))
                .build();
        } catch (ParseException e) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_PARSE_ACCESS_TOKEN, e);
        }
    }

    @Override
    public ModelAndView logout(String authHeaderValue) {
        String accessToken = authHeaderValue.replace("Bearer ", "");
        URI url = authenticationService.logout(accessToken);
        return new ModelAndView("redirect:" + url.toString());
    }

    private String parseEmailAddressFromAccessToken(String accessToken) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(accessToken);
        final List<String> emailAddresses = jwt.getJWTClaimsSet().getStringListClaim(EMAILS_CLAIM_NAME);
        return emailAddresses.get(0);
    }
}
