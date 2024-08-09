package uk.gov.hmcts.darts.testutils;

import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.common.config.security.JwksInitialize;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.TokenStub;

/**
 * A class that sets up wiremock jkws keys endpoint with a set of test based keys. This is designed to satisfy spring startup
 */
public class JwksWiremockInitialize implements JwksInitialize {

    private final TokenStub tokenStub = new TokenStub();

    @Autowired
    private ExternalAuthConfigurationProperties configurationProperties;

    @Override
    public void init() throws JOSEException {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        // populate the jkws keys endpoint with a valid generated key
        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());
    }
}