package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClientException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Disabled
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // temporary
class NotificationServiceTest {

    public static final String TEST_EMAIL_ADDRESS = "test@test.com";
    public static final String REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME = "request_to_transcriber";
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
    void scheduleNotificationOkConfirmEntryInDb() {
        String caseId = "scheduleNotification";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertTrue(result.getId() > 0);
        //        assertEquals("OPEN", result.getStatus());
        //        assertEquals(caseId, result.getCaseId());  CaseId or case number
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

        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        assertEquals(2, resultList.size());
        //        assertEquals(TEST_EMAIL_ADDRESS, resultList.get(0).getEmailAddress());
        //        assertEquals("test2@test.com", resultList.get(1).getEmailAddress());
    }

    @Test
    void scheduleNotificationInvalidEmail() {
        String caseId = "scheduleNotification";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test@.com")
            .templateValues("a json string")
            .build();
        service.scheduleNotification(request);
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        assertEquals(0, resultList.size());
    }

    @Test
    void sendNotificationToGovNotify() throws TemplateNotFoundException {
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "976bf288-705d-4cbb-b24f-c5529abf14cf");

        String caseId = "sendNotificationToGovNotify";
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
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
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertEquals("SENT", result.getStatus(), "Object may not have sent");

    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateId(TestInfo testInfo)
        throws TemplateNotFoundException, NotificationClientException {
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "INVALID-TEMPLATE-ID");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class))).thenThrow(new NotificationClientException(
            ""));
        String caseId = testInfo.getDisplayName();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertEquals("PROCESSING", result.getStatus());
        //        assertEquals(1, result.getAttempts());

    }

    @Test
    void sendNotificationToGovNotifyFailureRetryExceeded(TestInfo testInfo)
        throws TemplateNotFoundException, NotificationClientException {
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class))).thenThrow(new NotificationClientException(
            ""));
        String caseId = testInfo.getDisplayName();

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
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertEquals("FAILED", result.getStatus());
        //        assertEquals(3, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotifyInvalidJson(TestInfo testInfo) throws TemplateNotFoundException {
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)).thenReturn(
            "976bf288-1234-1234-1234-c5529abf14cf");//invalid template number
        String caseId = testInfo.getDisplayName();

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER_TEMPLATE_NAME)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("{,1,}")
            .build();
        service.scheduleNotification(request);

        service.sendNotificationToGovNotify();
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertEquals("FAILED", result.getStatus());
        //        assertEquals(0, result.getAttempts());
    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateName(TestInfo testInfo) throws TemplateNotFoundException {
        String caseId = testInfo.getDisplayName();
        when(templateIdHelper.findTemplateId(caseId)).thenThrow(new TemplateNotFoundException("oh no"));

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(caseId)
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues("{")
            .build();
        service.scheduleNotification(request);
        service.sendNotificationToGovNotify();
        //        List<Notification> resultList = notificationRepo.findByCaseId(caseId);
        //        Notification result = resultList.get(0);
        //        assertEquals("FAILED", result.getStatus());
        //        assertEquals(0, result.getAttempts());
        //        Mockito.verify(templateIdHelper).findTemplateId(Mockito.anyString());
    }


}
