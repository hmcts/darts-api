package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "h2db"})
class ResetPasswordIntTest {

    private static final String EXPECTED_REDIRECT_URL = "https://hmctsdartsb2csbox.b2clogin.com/hmctsdartsb2csbox.onmicrosoft.com/" +
        "B2C_1_darts_externaluser_password_reset/oauth2/v2.0/authorize?" +
        "client_id=dummy_client_id&redirect_uri=https%3A%2F%2Fexample.com%2Fhandle-oauth-code&" +
        "scope=openid&prompt=login&response_type=id_token";
    private static final String EXPECTED_REDIRECT_URL_WITH_OVERRIDE = "https://hmctsdartsb2csbox.b2clogin.com/hmctsdartsb2csbox.onmicrosoft.com/" +
        "B2C_1_darts_externaluser_password_reset/oauth2/v2.0/authorize?" +
        "client_id=dummy_client_id&redirect_uri=https%3A%2F%2Fdarts-portal.com%2Fauth%2Fcallback&" +
        "scope=openid&prompt=login&response_type=id_token";
    private static final String EXTERNAL_USER_RESET_PASSWORD_ENDPOINT = "/external-user/reset-password";
    private static final String EXTERNAL_USER_RESET_PASSWORD_ENDPOINT_WITH_OVERRIDE = "/external-user/reset-password?redirect_uri=https://darts-portal.com/auth/callback";


    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void resetPasswordShouldReturnRedirect() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_RESET_PASSWORD_ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                EXPECTED_REDIRECT_URL
            ));
    }
    
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void resetPasswordShouldReturnRedirectWithOverriddenRedirectUri() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_RESET_PASSWORD_ENDPOINT_WITH_OVERRIDE);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                EXPECTED_REDIRECT_URL_WITH_OVERRIDE
            ));
    }

}
