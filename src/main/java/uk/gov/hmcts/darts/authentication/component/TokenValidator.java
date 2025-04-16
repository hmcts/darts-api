package uk.gov.hmcts.darts.authentication.component;

import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;

@FunctionalInterface
public interface TokenValidator {

    JwtValidationResult validate(String accessToken, AuthProviderConfigurationProperties providerConfig, AuthConfigurationProperties configuration);

}
