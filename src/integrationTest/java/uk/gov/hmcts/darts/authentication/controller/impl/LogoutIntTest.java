package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.util.ReprovisionDatabaseBeforeEach;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LogoutIntTest extends IntegrationBase {

    private static final String EXTERNAL_USER_LOGOUT_ENDPOINT = "/external-user/logout";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void logoutShouldReturnRedirectWhenAccessTokenExists() throws Exception {
        String accessToken = "DummyAccessToken";

        String expectedUri = "https://hmctsdartsb2csbox.b2clogin.com/hmctsdartsb2csbox.onmicrosoft.com" +
            "/B2C_1_darts_externaluser_signin/oauth2/v2.0/logout?id_token_hint=" +
            accessToken +
            "&post_logout_redirect_uri=https%3A%2F%2Fdarts-portal.staging.platform.hmcts.net%2Fauth%2Flogout-callback";

        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGOUT_ENDPOINT)
            .header("Authorization", "Bearer " + accessToken);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                expectedUri
            ));
    }

}
