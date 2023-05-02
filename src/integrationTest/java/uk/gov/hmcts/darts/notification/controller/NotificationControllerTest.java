package uk.gov.hmcts.darts.notification.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.notification.entity.Notification;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"intTest", "test"})
class NotificationControllerTest {

    private static final String NOTIFICATION_CREATE_POST = "/notification/create";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    NotificationRepository notificationRepo;

    @BeforeEach
    void beforeEach() {
        notificationRepo.deleteAll();
    }

    @Test
    void saveToDb() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(NOTIFICATION_CREATE_POST);
        requestBuilder.queryParam("eventId", "court_manager_approve_transcript");
        requestBuilder.queryParam("caseId", "123456");
        requestBuilder.queryParam("emailAddresses", "test@test.com");

        ResultActions response = mockMvc.perform(requestBuilder);
        response.andExpect(status().isCreated());

        List<Notification> all = notificationRepo.findAll();
        Notification notificationRow = all.get(0);
        assertEquals("court_manager_approve_transcript", notificationRow.getEventId());
    }

}
