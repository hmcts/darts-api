package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class NotificationServiceTest extends IntegrationBase {

    public static final String TEST_EMAIL_ADDRESS = "test@test.com";
    public static final String REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME = "request_to_transcriber";
    @Autowired
    NotificationService service;
    @MockBean
    TemplateIdHelper templateIdHelper;
    @MockBean
    GovNotifyService govNotifyService;

    @Test
    void scheduleNotificationOkConfirmEntryInDb() {
        Integer caseId = dartsDatabase.hasSomeCourtCase().getId();

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity notification = resultList.get(0);
        assertTrue(notification.getId() > 0);
        assertEquals("OPEN", notification.getStatus());
        assertEquals(caseId, notification.getCourtCase().getId());
    }

    @Test
    void saveNotificationToDbMultipleEmails() {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test.com,test2@test.com")
            .templateValues("a json string")
            .build();

        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        assertEquals(2, resultList.size());
        assertEquals(TEST_EMAIL_ADDRESS, resultList.get(0).getEmailAddress());
        assertEquals("test2@test.com", resultList.get(1).getEmailAddress());
    }

    @Test
    void scheduleNotificationInvalidEmail() {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test@.com")
            .templateValues("a json string")
            .build();

        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        assertEquals(0, resultList.size());
    }

    @Test
    void sendNotificationToGovNotify() throws TemplateNotFoundException {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "976bf288-705d-4cbb-b24f-c5529abf14cf");
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues(
                  """
                  {
                    "key1": "value1",
                    "key2": "value2",
                    "key3": "value3",
                    "key4": "value4",
                    "key5": "value5"
                  }
                  """)
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.get(0);
        assertEquals("SENT", result.getStatus(), "Object may not have sent");
    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateId() throws TemplateNotFoundException, NotificationClientException {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME))
              .thenReturn("INVALID-TEMPLATE-ID");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class)))
              .thenThrow(new NotificationClientException(""));
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("")
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.get(0);
        assertEquals("PROCESSING", result.getStatus());
        assertEquals(1, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotifyFailureRetryExceeded() throws TemplateNotFoundException, NotificationClientException {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME))
              .thenReturn("976bf288-1234-1234-1234-c5529abf14cf");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class)))
              .thenThrow(new NotificationClientException(""));

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("")
            .build();
        service.scheduleNotification(request);

        for (int counter = 0; counter <= 3; counter++) {
            service.sendNotificationToGovNotify();
        }

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(3, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotifyInvalidJson() throws TemplateNotFoundException {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");//invalid template number

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("{,1,}")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(0, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateName() throws TemplateNotFoundException {
        var caseId = dartsDatabase.hasSomeCourtCase().getId();
        when(templateIdHelper.findTemplateId("invalid"))
              .thenThrow(new TemplateNotFoundException("oh no"));
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("invalid")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("{")
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.get(0);
        assertEquals("FAILED", result.getStatus());
        assertEquals(0, result.getAttempts());
        verify(templateIdHelper).findTemplateId(anyString());
    }
}
