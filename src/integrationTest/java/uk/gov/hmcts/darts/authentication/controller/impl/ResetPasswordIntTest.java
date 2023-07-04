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
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "postgresTestContainer"})
class ResetPasswordIntTest {

    private static final String EXPECTED_REDIRECT_URL = "https://hmctsdartsb2csbox.b2clogin.com/hmctsdartsb2csbox.onmicrosoft.com/" +
        "B2C_1_darts_externaluser_password_reset/oauth2/v2.0/authorize?" +
        "client_id=dummy_client_id&redirect_uri=https%3A%2F%2Fexample.com%2Fhandle-oauth-code&" +
        "scope=openid&prompt=login&response_type=id_token";
    private static final String EXTERNAL_USER_RESET_PASSWORD_ENDPOINT = "/external-user/reset-password";

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private MediaRequestRepository mediaRequestRepository;

    @MockBean
    private DailyListRepository dailyListRepository;

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

}
