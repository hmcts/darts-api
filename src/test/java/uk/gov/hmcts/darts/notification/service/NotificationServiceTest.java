package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.entity.Notification;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Autowired
    NotificationService service;

    @Autowired
    NotificationRepository notificationRepo;

    @MockBean
    TemplateIdHelper templateIdHelper;

    @MockBean
    GovNotifyService govNotifyService;

    @BeforeEach
    void beforeEach() {
        notificationRepo.deleteAll();
    }

    @Test
    void scheduleNotification() {
        String caseId = "scheduleNotification";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertTrue(result.getId() > 0);
        assertEquals("OPEN", result.getStatus(), "Object may not have been enriched properly");
        assertEquals(caseId, result.getCaseId(), "Object not populated properly");
    }

    @Test
    void saveNotificationToDbMultipleEmails() {
        String caseId = "saveNotificationToDbMultipleEmails";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test.com,test2@test.com")
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);

        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        assertEquals(2, resultList.size(), "number of records is wrong.");
        assertEquals("test@test.com", resultList.get(0).getEmailAddress(), "first email address is wrong");
        assertEquals("test2@test.com", resultList.get(1).getEmailAddress(), "");
    }

    @Test
    void scheduleNotification_invalidEmail() {
        String caseId = "scheduleNotification";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test@.com")
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        assertEquals(0, resultList.size());
    }

    @Test
    void sendNotificationToGovNotify() throws TemplateNotFoundException {
        when(templateIdHelper.findTemplateId("request_to_transcriber")).thenReturn(
            "976bf288-705d-4cbb-b24f-c5529abf14cf");

        String caseId = "sendNotificationToGovNotify";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("request_to_transcriber")
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("{\n" +
                            "  \"key1\": \"value1\",\n" +
                                "  \"key2\": \"value2\",\n" +
                                "  \"key3\": \"value3\",\n" +
                                "  \"key4\": \"value4\",\n" +
                                "  \"key5\": \"value5\"\n" +
                                "}")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertEquals("SENT", result.getStatus(), "Object may not have sent");

    }

    @Test
    void sendNotificationToGovNotify_invalidTemplateId(TestInfo testInfo) throws TemplateNotFoundException, NotificationClientException {
        when(templateIdHelper.findTemplateId("request_to_transcriber")).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");//invalid tempalte number
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class))).thenThrow(new NotificationClientException(
            ""));//invalid template id
        String caseId = testInfo.getDisplayName();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("request_to_transcriber")
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertEquals("PROCESSING", result.getStatus());
        assertEquals(1, result.getAttempts());

    }

    @Test
    void sendNotificationToGovNotify_failure_retryExceeded(TestInfo testInfo) throws TemplateNotFoundException, NotificationClientException {
        when(templateIdHelper.findTemplateId("request_to_transcriber")).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");//invalid template id
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class))).thenThrow(new NotificationClientException(
            ""));//invalid template id
        String caseId = testInfo.getDisplayName();

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("request_to_transcriber")
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("")
            .build();
        service.scheduleNotification(request);
        for (int counter = 0; counter <= 3; counter++) {
            service.sendNotificationToGovNotify();
        }
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(3, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotify_invalidJson(TestInfo testInfo) throws TemplateNotFoundException {
        when(templateIdHelper.findTemplateId("request_to_transcriber")).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");//invalid template number
        String caseId = testInfo.getDisplayName();

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("request_to_transcriber")
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("{,1,}")
            .build();
        service.scheduleNotification(request);

        service.sendNotificationToGovNotify();
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(0, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotify_invalidTemplateName(TestInfo testInfo) throws TemplateNotFoundException {
        String caseId = testInfo.getDisplayName();
        when(templateIdHelper.findTemplateId(caseId)).thenThrow(new TemplateNotFoundException("oh no"));

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(caseId)
            .caseId(caseId)
            .emailAddresses("test@test.com")
            .templateValues("{")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();
        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        Notification result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(0, result.getAttempts());
        Mockito.verify(templateIdHelper).findTemplateId(Mockito.anyString());
    }


}
