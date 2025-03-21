package uk.gov.hmcts.darts.authentication.component.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

import java.util.Arrays;
import java.util.HashSet;

@Component
public class JwtProcessorCache {

    @Cacheable("defaultJWTProcessor")
    public DefaultJWTProcessor<SecurityContext> getDefaultJwtProcessor(AuthProviderConfigurationProperties providerConfig,
                                                                       AuthConfigurationProperties configuration) {
        var keySelector = new JWSVerificationKeySelector<>(
            JWSAlgorithm.RS256,
            providerConfig.getJwkSource()
        );

        var jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(keySelector);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .issuer(configuration.getIssuerUri())
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
                configuration.getClaims()
            ))
        );
        jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);
        return jwtProcessor;
    }
}
