package uk.gov.hmcts.darts.authentication.component.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;

import java.text.ParseException;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class TokenValidatorImpl implements TokenValidator {

    private final JwtProcessorCache jwtProcessorCache;

    @Override
    public JwtValidationResult validate(String accessToken, AuthProviderConfigurationProperties providerConfig, AuthConfigurationProperties configuration) {
        log.debug("Validating JWT: {}", accessToken);

        try {
            jwtProcessorCache.getDefaultJwtProcessor(providerConfig, configuration).process(accessToken, null);
            log.debug("JWT Token Validation successful");
        } catch (ParseException | JOSEException | BadJOSEException e) {
            log.error("JWT Token Validation failed", e);
            return new JwtValidationResult(false, e.getMessage());
        }
        return new JwtValidationResult(true, null);
    }
}