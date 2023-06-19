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
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "h2db"})
class LoginOrRefreshIntTest {

    private static final String EXPECTED_LOGIN_REDIRECT_URL = "http://localhost:8080/oauth2/v2.0/authorize?client_id=dummy_client_id&response_type=code&redirect_uri=https%3A%2F%2Fexample.com%2Fhandle-oauth-code&response_mode=form_post&scope=openid&prompt=login";
    private static final String EXTERNAL_USER_LOGIN_OR_REFRESH_ENDPOINT = "/external-user/login-or-refresh";

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private AudioRequestRepository audioRequestRepository;

    @MockBean
    private CourthouseRepository courthouseRepository;
    @MockBean
    private DailyListRepository dailyListRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void loginOrRefreshShouldReturnRedirectWhenNoSessionIdQueryParamIsSent() throws Exception {
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

