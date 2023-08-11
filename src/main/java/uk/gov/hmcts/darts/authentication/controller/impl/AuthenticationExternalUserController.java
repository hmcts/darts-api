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
@RequiredArgsConstructor
public class AuthenticationExternalUserController implements AuthenticationController {

    private static final String EMAILS_CLAIM_NAME = "emails";

    private final AuthenticationService authenticationService;
    private final AuthorisationApi authorisationApi;

    @Override
    public ModelAndView loginOrRefresh(String authHeaderValue, String redirectUri) {
        String accessToken = null;
        if (authHeaderValue != null) {
            accessToken = authHeaderValue.replace("Bearer ", "");
        }
        URI url = authenticationService.loginOrRefresh(accessToken, redirectUri);
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public SecurityToken handleOauthCode(String code) {
        String accessToken = authenticationService.handleOauthCode(code);
        var securityTokenBuilder = SecurityToken.builder()
            .accessToken(accessToken);

        try {
            Optional<String> emailAddressOptional = parseEmailAddressFromAccessToken(accessToken);
            if (emailAddressOptional.isPresent()) {
                Optional<UserState> userStateOptional = authorisationApi.getAuthorisation(emailAddressOptional.get());
                securityTokenBuilder.userState(userStateOptional.orElse(null));
            }
        } catch (ParseException e) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_PARSE_ACCESS_TOKEN, e);
        }

        return securityTokenBuilder.build();
    }

    @Override
    public ModelAndView logout(String authHeaderValue, String redirectUri) {
        String accessToken = authHeaderValue.replace("Bearer ", "");
        URI url = authenticationService.logout(accessToken, redirectUri);
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public ModelAndView resetPassword(String redirectUri) {
        URI url = authenticationService.resetPassword(redirectUri);
        return new ModelAndView("redirect:" + url.toString());
    }

    private Optional<String> parseEmailAddressFromAccessToken(String accessToken) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(accessToken);
        final List<String> emailAddresses = jwt.getJWTClaimsSet().getStringListClaim(EMAILS_CLAIM_NAME);
        if (emailAddresses == null || emailAddresses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(emailAddresses.get(0));
    }
}
