package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;
import wiremock.org.eclipse.jetty.util.UrlEncoded;

import java.net.URLEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LogoutIntTest extends IntegrationBaseWithWiremock {

    private static final String EXTERNAL_USER_LOGOUT_ENDPOINT = "/external-user/logout";
    private static final String EXTERNAL_USER_LOGOUT_ENDPOINT_WITH_OVERRIDE = "/external-user/logout?redirect_uri=https://darts-portal.com/auth/logout-callback";

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.logout-redirect-uri}")
    private String logoutRedirectUri;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logoutShouldReturnRedirectWhenAccessTokenExists() throws Exception {
        String accessToken = "DummyAccessToken";

        String expectedUri = "http://localhost:" + wiremockPort + "/B2C_1_darts_externaluser_signin/oauth2/v2.0/logout?id_token_hint=" +
                             accessToken +
                             "&post_logout_redirect_uri=" + URLEncoder.encode(logoutRedirectUri);

        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGOUT_ENDPOINT)
            .header("Authorization", "Bearer " + accessToken);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                expectedUri
            ));
    }

    @Test
    void logoutShouldReturnRedirectWithOverriddenRedirectUri() throws Exception {
        String accessToken = "DummyAccessToken";

        String expectedUri = "http://localhost:" + wiremockPort + "/B2C_1_darts_externaluser_signin/oauth2/v2.0/logout?id_token_hint=" +
                             accessToken +
                             "&post_logout_redirect_uri=https%3A%2F%2Fdarts-portal.com%2Fauth%2Flogout-callback";

        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGOUT_ENDPOINT_WITH_OVERRIDE)
            .header("Authorization", "Bearer " + accessToken);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                expectedUri
            ));
    }

}