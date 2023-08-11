package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LoginOrRefreshIntTest extends IntegrationBase {

    private static final String EXPECTED_LOGIN_REDIRECT_URL = "http://localhost:8080/oauth2/v2.0/authorize?client_id=dummy_client_id&redirect_uri=https%3A%2F%2Fexample.com%2Fhandle-oauth-code&scope=openid&prompt=login&response_mode=form_post&response_type=code";
    private static final String EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT = "/external-user/login-or-refresh";
    private static final String EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE = "/external-user/login-or-refresh?redirect_uri=https://darts-portal.com/auth/callback";
    private static final String EXPECTED_LOGIN_REDIRECT_URL_WITH_OVERRIDE = "http://localhost:8080/oauth2/v2.0/authorize?client_id=dummy_client_id&redirect_uri=https%3A%2F%2Fdarts-portal.com%2Fauth%2Fcallback&scope=openid&prompt=login&response_mode=form_post&response_type=code";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void loginOrRefreshShouldReturnRedirectWhenNoAuthHeaderIsSent() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                EXPECTED_LOGIN_REDIRECT_URL
            ));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void loginOrRefreshShouldReturnRedirectWithOverriddenRedirectUri() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT_WITH_OVERRIDE);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                EXPECTED_LOGIN_REDIRECT_URL_WITH_OVERRIDE
            ));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void loginOrRefreshShouldReturnRedirectWhenNoSessionExistsInCache() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT)
            .queryParam("session-id", UUID.randomUUID().toString());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                EXPECTED_LOGIN_REDIRECT_URL
            ));
    }

}

