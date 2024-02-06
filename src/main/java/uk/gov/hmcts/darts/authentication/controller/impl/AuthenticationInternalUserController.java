package uk.gov.hmcts.darts.authentication.controller.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;

import java.text.ParseException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/internal-user")
public class AuthenticationInternalUserController extends AbstractUserController {

    public AuthenticationInternalUserController(AuthenticationService authenticationService, AuthorisationApi authorisationApi,
          AuthStrategySelector locator) {
        super(locator, authenticationService, authorisationApi);
    }

    @Override
    Optional<String> parseEmailAddressFromAccessToken(String accessToken) throws ParseException {
        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();
        SignedJWT jwt = SignedJWT.parse(accessToken);
        final String emailAddresses = jwt.getJWTClaimsSet().getStringClaim(configStrategy.getConfiguration().getClaims());
        if (emailAddresses == null || emailAddresses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(emailAddresses);
    }
}
