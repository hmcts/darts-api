package uk.gov.hmcts.darts.authentication.controller.impl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.testutils.DartsTokenAndJwksKey;
import uk.gov.hmcts.darts.testutils.DartsTokenGenerator;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the spring token validation layer {@link uk.gov.hmcts.darts.common.config.security.SecurityConfig}.
 */
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "h2db", "in-memory-caching", "tokenSecurityTest"})
class TokenValidatorTest extends IntegrationBaseWithWiremock {
    @Autowired
    private ExternalAuthProviderConfigurationProperties configurationProviderProperties;

    @Autowired
    private ExternalAuthConfigurationProperties configurationProperties;

    private static final String ENDPOINT_URL = "/admin/security-groups";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountStub userAccountStub;

    @AfterEach
    public void after() {
        userAccountStub.setActiveState("darts.global.user@hmcts.net", true);
    }

    @Test
    void testInvalidIssuer() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer("test")
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", Matchers.containsString("Invalid issuer")))
            .andReturn();
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
    void checkTokenExpiry() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().useGlobalKey(true).issuer(configurationProperties.getIssuerUri()).useExpiredToken(true)
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().isUnauthorized())
            .andReturn();
    }

    @Test
    void testNoToken() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer("test")
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().exists("Location"))
            .andReturn();
    }

    @Test
    void testInactiveUserCheck() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        userAccountStub.setActiveState("darts.global.user@hmcts.net", false);

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().isForbidden())
            .andExpect(content().json("{\"type\":\"AUTHORISATION_114\",\"title\":\"User is not active\",\"status\":403}"))
            .andReturn();
    }

    @Test
    void testSuccessfulCall() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().useGlobalKey(true).issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        mockMvc.perform(get(ENDPOINT_URL).header("Authorization", "Bearer " + tokenDetails.getToken()))
            .andExpect(status().is2xxSuccessful());
    }

    // TODO: We need to enable audience checking in our security layer
    @SuppressWarnings({"PMD.DetachedTestCase", "PMD.SignatureDeclareThrowsException"})
    //@Test
    void testInvalidAudience() throws Exception {
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
}