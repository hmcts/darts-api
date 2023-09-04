package uk.gov.hmcts.darts.authentication.component.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.model.JwtValidationResult;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenValidatorImpl implements TokenValidator {

    private static final String EMAILS_CLAIM_NAME = "emails";

    @Override
    public JwtValidationResult validate(String accessToken, AuthProviderConfigurationProperties providerConfig, AuthConfigurationProperties configuration) {
        log.debug("Validating JWT: {}", accessToken);

        var keySelector = new JWSVerificationKeySelector<>(
            JWSAlgorithm.RS256,
            providerConfig.getJwkSource()
        );

        var jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(keySelector);

        JWTClaimsSet jwtClaimsSet = new Builder()
            .issuer(configuration.getIssuerURI())
            .build();
        var claimsVerifier = new DefaultJWTClaimsVerifier<>(
            configuration.getClientId(),
            jwtClaimsSet,
            new HashSet<>(Arrays.asList(
                JWTClaimNames.AUDIENCE,
                JWTClaimNames.ISSUER,
                JWTClaimNames.EXPIRATION_TIME,
                JWTClaimNames.ISSUED_AT,
                JWTClaimNames.SUBJECT,
                EMAILS_CLAIM_NAME
            ))
        );
        jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);

        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(accessToken, null);
            log.debug("Validation successful - emailAddresses: {}", claimsSet.getStringListClaim(EMAILS_CLAIM_NAME));
        } catch (ParseException | JOSEException | BadJOSEException e) {
            log.debug("Validation failed", e);
            return new JwtValidationResult(false, e.getMessage());
        }

        return new JwtValidationResult(true, null);
    }

}
