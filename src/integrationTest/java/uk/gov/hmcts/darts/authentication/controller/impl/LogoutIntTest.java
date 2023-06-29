package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.SessionService;
import uk.gov.hmcts.darts.common.util.ClearDatabase;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "h2db"})
@ClearDatabase
class LogoutIntTest {

    private static final String EXTERNAL_USER_LOGOUT_ENDPOINT = "/external-user/logout";

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private MediaRequestRepository mediaRequestRepository;

    @MockBean
    private CourthouseRepository courthouseRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void logoutShouldReturnRedirectWhenSessionExists() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        String id = mockHttpSession.getId();

        sessionService.putSession(id, new Session(id, null, 0));

        String expectedUri = "https://hmctsdartsb2csbox.b2clogin.com/hmctsdartsb2csbox.onmicrosoft.com" +
            "/B2C_1_darts_externaluser_signin/oauth2/v2.0/logout?id_token_hint=" +
            id +
            "&post_logout_redirect_uri=https%3A%2F%2Fdarts-portal.staging.platform.hmcts.net%2Fauth%2Flogout-callback";

        MockHttpServletRequestBuilder requestBuilder = get(EXTERNAL_USER_LOGOUT_ENDPOINT)
            .session(mockHttpSession);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isFound())
            .andExpect(header().string(
                HttpHeaders.LOCATION,
                expectedUri
            ));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void logoutShouldReturnErrorResponseWhenSessionDoesNotExist() throws Exception {
        MvcResult response = mockMvc.perform(get(EXTERNAL_USER_LOGOUT_ENDPOINT))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"AUTHENTICATION_102",
                "title":"Logout was attempted for a session that was inactive",
                "status":500
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

}
