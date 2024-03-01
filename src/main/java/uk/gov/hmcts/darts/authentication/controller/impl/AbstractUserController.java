package uk.gov.hmcts.darts.authentication.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.UserAccountService;

import java.net.URI;
import java.text.ParseException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public abstract class AbstractUserController implements AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuthorisationApi authorisationApi;
    protected final AuthStrategySelector locator;
    private final UserAccountService userAccountService;

    abstract Optional<String> parseEmailAddressFromAccessToken(String accessToken) throws ParseException;

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
                if (userStateOptional.isPresent()) {
                    var userState = userStateOptional.get();
                    securityTokenBuilder.userState(userState);
                    userAccountService.updateLastLoginTime(userState.getUserId());
                }
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
}
