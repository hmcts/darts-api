package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.SessionService;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "h2db"})
class InvalidateSessionIntTest {

    private static final String EXTERNAL_USER_INVALIDATE_SESSION_ENDPOINT = "/external-user/invalidate-session";

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private AudioRequestRepository audioRequestRepository;

    @MockBean
    private CourthouseRepository courthouseRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void invalidateSessionShouldReturn200OkWhenSessionIsInvalidated() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        String id = mockHttpSession.getId();
        sessionService.putSession(id, new Session(id, null, 0));

        MockHttpServletRequestBuilder requestBuilder = post(EXTERNAL_USER_INVALIDATE_SESSION_ENDPOINT)
            .session(mockHttpSession);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());
    }

}
