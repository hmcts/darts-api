package uk.gov.hmcts.darts.authentication.component;

import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;

public interface TokenValidator {

    JwtValidationResult validate(String accessToken,  AuthConfiguration<?> configuration);

}
