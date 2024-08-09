package uk.gov.hmcts.darts.authentication.controller.impl.token;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.testutils.DartsTokenAndJwksKey;
import uk.gov.hmcts.darts.testutils.DartsTokenGenerator;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.conf.TokenConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the spring  validation layer. See {@link uk.gov.hmcts.darts.authentication.component.impl.TokenValidatorImpl}
 */
@AutoConfigureMockMvc
@ImportAutoConfiguration({TokenConfiguration.class})
@ActiveProfiles({"intTest", "h2db", "in-memory-caching", "tokenSecurityTest"})
class TokenValidatorTest extends IntegrationBase {
    @Autowired
    private ExternalAuthProviderConfigurationProperties configurationProviderProperties;

    @Autowired
    private ExternalAuthConfigurationProperties configurationProperties;

    private static final String EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE = "/external-user/login-or-refresh?redirect_uri=https://darts-portal.com/auth/callback";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkTokenExpiry() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
             DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri()).useExpiredToken(true)
                 .audience(configurationProperties.getClientId()).build();
             DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

             tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

            MockHttpServletRequestBuilder requestBuilder
                = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", tokenDetails.getToken());

            mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection());
             Assertions.assertFalse(LogUtil.getMemoryLogger()
                                        .searchLogs("JWT Token Validation failed", "Expired JWT", Level.ERROR).isEmpty());
        }
        );
    }

    @Test
    void checkInvalidAudience() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
                 DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri()).useExpiredToken(true)
                     .audience("invalid").build();
                 DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

                 tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

                 MockHttpServletRequestBuilder requestBuilder
                     = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", tokenDetails.getToken());

                 mockMvc.perform(requestBuilder)
                     .andExpect(status().is3xxRedirection());
                 Assertions.assertFalse(LogUtil.getMemoryLogger()
                                            .searchLogs("JWT Token Validation failed", "JWT audience rejected: [invalid]", Level.ERROR).isEmpty());
             }
        );
    }

    @Test
    void checkInvalidIssuer() throws Exception {
        runWhenExpectingExternalJwksRefresh(() -> {
             DartsTokenGenerator token = DartsTokenGenerator.builder().issuer("invalidissuer").useExpiredToken(true)
                 .audience(configurationProperties.getClientId()).build();
             DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

             tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

             MockHttpServletRequestBuilder requestBuilder
                 = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", tokenDetails.getToken());

             mockMvc.perform(requestBuilder)
                 .andExpect(status().is3xxRedirection());
             Assertions.assertFalse(LogUtil.getMemoryLogger()
                                        .searchLogs("JWT Token Validation failed",
                                                    "JWT iss claim has value invalidissuer, must be dummy_issuer_uri", Level.ERROR).isEmpty());
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

        MockHttpServletRequestBuilder requestBuilder
            = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", tokenDetails.getToken());

        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection());

        Assertions.assertFalse(LogUtil.getMemoryLogger()
                                   .searchLogs("JWT Token Validation failed", "Signed JWT rejected: Invalid signature", Level.ERROR).isEmpty());
    }

    @Test
    void testWithTokenSignedByGlobalKey() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).useGlobalKey(true).build();

        MockHttpServletRequestBuilder requestBuilder
            = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", token.fetchTokenWithGlobalUser().getToken());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                "/")
            );
    }

    @Test
    void checkRefreshOfPublicKeys() throws Exception {
        // make sure we leave enough time for the refresh between runs
        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        runWhenExpectingExternalJwksRefresh(() -> successfulLoginWithValidation());

        // a total of 3 public key fetches should be seen based on the configuration properties
        tokenStub.verifyNumberOfTimesKeysObtained(3);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void successfulLoginWithValidation() throws Exception {
        DartsTokenGenerator token = DartsTokenGenerator.builder().issuer(configurationProperties.getIssuerUri())
            .audience(configurationProperties.getClientId()).build();
        DartsTokenAndJwksKey tokenDetails = token.fetchTokenWithGlobalUser();

        tokenStub.stubExternalJwksKeys(tokenDetails.getJwksKey());

        MockHttpServletRequestBuilder requestBuilder
            = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE).header("Authorization", tokenDetails.getToken());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                "/")
                );
    }
}