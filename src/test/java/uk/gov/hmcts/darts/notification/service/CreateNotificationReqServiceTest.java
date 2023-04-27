package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.darts.notification.dto.CreateNotificationReq;
import uk.gov.hmcts.darts.notification.entity.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CreateNotificationReqServiceTest {

    @Autowired
    NotificationService service;

    @Test
    void createNotification() {
        CreateNotificationReq createNotificationReq = new CreateNotificationReq();
        createNotificationReq.setEventId("An eventId");
        createNotificationReq.setCaseId("A caseId");
        createNotificationReq.setEmailAddress("test@test.com");
        createNotificationReq.setTemplateValues("a json string");
        Notification result = service.sendNotification(createNotificationReq);
        assertEquals(1, result.getId(), "Database may not have been hit");
        assertEquals("OPEN", result.getStatus(), "Object may not have been enriched properly");
        assertEquals("A caseId", result.getCaseId(), "Object not populated properly");
    }


}
