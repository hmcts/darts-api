package uk.gov.hmcts.darts.notification.service;

import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.service.notify.NotificationClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.REQUEST_TO_TRANSCRIBER;

@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
class NotificationServiceTest extends IntegrationBase {

    private static final int SYSTEM_USER_ID = 0;
    private static final String TEST_EMAIL_ADDRESS = "test@test.com";

    @Autowired
    NotificationService service;
    @MockitoBean
    TemplateIdHelper templateIdHelper;
    @MockitoBean
    GovNotifyService govNotifyService;

    @MockitoBean
    LogApi logApi;

    @Test
    void scheduleNotificationOkConfirmEntryInDb() {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .build();
        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity notification = resultList.getFirst();
        assertTrue(notification.getId() > 0);
        assertEquals(NotificationStatus.OPEN, notification.getStatus());
        assertEquals(caseId, notification.getCourtCase().getId());

        assertEquals(SYSTEM_USER_ID, notification.getCreatedBy().getId());
        assertEquals(SYSTEM_USER_ID, notification.getLastModifiedBy().getId());

        verify(logApi, times(1)).scheduleNotification(any(), any());
    }

    @Test
    void saveNotificationToDbMultipleEmails() {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("test@test.com,test2@test.com")
            .build();

        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        assertEquals(2, resultList.size());
        assertEquals(TEST_EMAIL_ADDRESS, resultList.getFirst().getEmailAddress());
        assertEquals("test2@test.com", resultList.get(1).getEmailAddress());
    }

    @Test
    void sendNotificationToGovNotifyNow() throws TemplateNotFoundException {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER.toString())).thenReturn(
            "976bf288-705d-4cbb-b24f-c5529abf14cf");
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("key1", "value1");
        templateParams.put("key2", "value2");
        templateParams.put("key3", "value3");
        templateParams.put("key4", "value4");
        templateParams.put("key5", "value5");

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER.toString())
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues(templateParams)
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotifyNow();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.getFirst();
        assertEquals(NotificationStatus.SENT, result.getStatus(), "Object may not have sent");

        verify(logApi, times(1)).scheduleNotification(any(), any());
        verify(logApi, times(1)).sentNotification(any(), any(), any());
    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateId() throws TemplateNotFoundException, NotificationClientException {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER.toString()))
            .thenReturn("INVALID-TEMPLATE-ID");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class)))
            .thenThrow(new NotificationClientException(""));
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER.toString())
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotifyNow();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.getFirst();
        assertEquals(NotificationStatus.PROCESSING, result.getStatus());
        assertEquals(1, result.getAttempts());

        verify(logApi, times(1)).scheduleNotification(any(), any());
        verify(logApi, times(1)).sendingNotification(any(), any(), any());
        verify(logApi, times(1)).errorRetryingNotification(any(), any(), any());
    }

    @Test
    void sendNotificationToGovNotifyFailureRetryExceeded() throws TemplateNotFoundException, NotificationClientException {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER.toString()))
            .thenReturn("976bf288-1234-1234-1234-c5529abf14cf");
        when(govNotifyService.sendNotification(any(GovNotifyRequest.class)))
            .thenThrow(new NotificationClientException(""));

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER.toString())
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .build();
        service.scheduleNotification(request);

        for (int counter = 0; counter <= 3; counter++) {
            service.sendNotificationToGovNotifyNow();
        }

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.getFirst();
        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertEquals(3, result.getAttempts());

        verify(logApi, times(1)).scheduleNotification(any(), any());
        verify(logApi, times(4)).sendingNotification(any(), any(), any());
        verify(logApi, times(3)).errorRetryingNotification(any(), any(), any());
        verify(logApi, times(1)).failedNotification(any(), any(), any());
    }

    @Test
    void sendNotificationToGovNotifyInvalidTemplateName() throws TemplateNotFoundException {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId("invalid"))
            .thenThrow(new TemplateNotFoundException("oh no"));
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("invalid")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .build();

        service.scheduleNotification(request);
        service.sendNotificationToGovNotifyNow();

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        NotificationEntity result = resultList.getFirst();
        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertEquals(0, result.getAttempts());
        verify(templateIdHelper).findTemplateId(anyString());
    }

    @Test
    void sendNotificationUsingUserAccounts() {
        var userAccountEntities = generateUserAccountEntities();

        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .userAccountsToEmail(userAccountEntities)
            .build();
        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        List<String> emailList = resultList.stream().map(NotificationEntity::getEmailAddress).toList();
        assertTrue(emailList.contains(userAccountEntities.getFirst().getEmailAddress()));
        assertTrue(emailList.contains(userAccountEntities.get(1).getEmailAddress()));
    }

    @Test
    void sendNotificationChecksUserAccountActiveStatus() {
        var userAccountEntities = generateUserAccountEntities();
        userAccountEntities.getFirst().setActive(false);

        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .userAccountsToEmail(userAccountEntities)
            .build();
        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        List<String> emailList = resultList.stream().map(NotificationEntity::getEmailAddress).toList();
        assertFalse(emailList.contains(userAccountEntities.getFirst().getEmailAddress()));
        assertTrue(emailList.contains(userAccountEntities.get(1).getEmailAddress()));
    }

    @Test
    void sendNotificationUsingUserAccountsAnEmails() {
        var userAccountEntities = generateUserAccountEntities();

        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("An eventId")
            .caseId(caseId)
            .emailAddresses("testEmail3@test.com, testEmail4@test.com")
            .userAccountsToEmail(userAccountEntities)
            .build();
        service.scheduleNotification(request);

        List<NotificationEntity> resultList = dartsDatabase.getNotificationsForCase(caseId);
        List<String> emailList = resultList.stream().map(NotificationEntity::getEmailAddress).toList();
        assertTrue(emailList.contains(userAccountEntities.getFirst().getEmailAddress()));
        assertTrue(emailList.contains(userAccountEntities.get(1).getEmailAddress()));
        assertTrue(emailList.contains("testEmail3@test.com"));
        assertTrue(emailList.contains("testEmail4@test.com"));
    }

    @Test
    void sendWrongTypeOfMap() throws TemplateNotFoundException {
        var caseId = dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId(REQUEST_TO_TRANSCRIBER.toString())).thenReturn(
            "976bf288-705d-4cbb-b24f-c5529abf14cf");
        Map<String, String> templateParams = new LinkedMap<>();
        templateParams.put("key1", "value1");
        templateParams.put("key2", "value2");
        templateParams.put("key3", "value3");
        templateParams.put("key4", "value4");
        templateParams.put("key5", "value5");

        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER.toString())
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues(templateParams)
            .build();

        service.scheduleNotification(request);
    }

    private List<UserAccountEntity> generateUserAccountEntities() {
        UserAccountEntity userAccount1 = new UserAccountEntity();
        userAccount1.setId(10);
        userAccount1.setActive(true);
        userAccount1.setEmailAddress("testEmail1@test.com");
        UserAccountEntity userAccount2 = new UserAccountEntity();
        userAccount2.setId(11);
        userAccount2.setActive(true);
        userAccount2.setEmailAddress("testEmail2@test.com");
        return List.of(userAccount1, userAccount2);
    }
}