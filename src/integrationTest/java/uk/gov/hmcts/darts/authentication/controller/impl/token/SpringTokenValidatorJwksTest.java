package uk.gov.hmcts.darts.authentication.controller.impl.token;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.testutils.DartsTokenAndJwksKey;
import uk.gov.hmcts.darts.testutils.DartsTokenGenerator;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.conf.TokenConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the spring token validation layer {@link uk.gov.hmcts.darts.common.config.security.SecurityConfig}.
 */
@AutoConfigureMockMvc
@ImportAutoConfiguration({TokenConfiguration.class})
@ActiveProfiles({"intTest", "h2db", "in-memory-caching", "tokenSecurityTest"})
class SpringTokenValidatorJwksTest extends IntegrationBase {
    @Autowired
    private ExternalAuthProviderConfigurationProperties configurationProviderProperties;

    @Autowired
    private ExternalAuthConfigurationProperties configurationProperties;

    private static final String ENDPOINT_URL = "/admin/security-groups";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testInvalidIssuer() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
             DartsTokenGenerator token = DartsTokenGenerator.builder().issuer("test")
                 .audience(configurationProperties.getClientId()).build();
             DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

             tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

             mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
                .andExpect(status().isUnauthorized())
                 .andExpect(header().string("WWW-Authenticate", Matchers.containsString("Invalid issuer")))
                 .andReturn();
            }
        );
    }

    // TODO: Revisit this when we fix the functional tests
    @SuppressWarnings({"PMD.DetachedTestCase", "PMD.SignatureDeclareThrowsException"})
    //@Test
    void testInvalidAudience() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
             DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
                 .audience("client").build();
             DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

             tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

             mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
                 .andExpect(status().isUnauthorized())
                 .andExpect(header().string("WWW-Authenticate",
                                            Matchers.containsString("An error occurred while attempting to decode the Jwt: The aud claim is not valid")))
                 .andReturn();
         }
        );
    }

    @Test
    void checkInvalidTokenSignature() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        // get another token and use that breaking the signature
        token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        tokenDetails = token.fetchTokenWithGlobalUser();

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate",
                                       Matchers.containsString("An error occurred while attempting to decode the Jwt: Signed JWT rejected: Invalid signature")))
            .andReturn();
        }

    @Test
    void checkRefreshOfJwksPublicKeys() throws Exception {
        // make sure we leave enough time for the refresh between runs
        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        // a total of 3 public key fetches should be seen based on the configuration properties
        tokenStub.verifyNumberOfTimesKeysObtained(3);
    }

    @Test
    void checkTokenExpiry() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
             DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri()).useExpiredToken(true)
                 .audience(configurationProperties.getClientId()).build();
             DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

             tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

             mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
                 .andExpect(status().isUnauthorized())
                 .andExpect(header().string("WWW-Authenticate", Matchers.containsString("An error occurred while attempting to decode the Jwt: Expired JWT")))
                 .andReturn();
         }
        );
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    void successfulLoginWithValidation() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().isOk())
            .andReturn();
    }
}