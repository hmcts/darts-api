package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.ACCEPT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AUTHORISE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.COMPLETE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REJECT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REJECTION_REASON;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.COURT_MANAGER_APPROVE_TRANSCRIPT;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.REQUEST_TO_TRANSCRIBER;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_AVAILABLE;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_APPROVED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_REJECTED;

@ExtendWith(MockitoExtension.class)
class TranscriptionNotificationsTest {

    @InjectMocks
    private TranscriptionNotifications transcriptionNotifications;

    @Mock
    private TranscriptionEntity transcriptionEntity;

    @Mock
    private NotificationApi notificationApi;

    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private AuditApi auditApi;

    @Captor
    private ArgumentCaptor<SaveNotificationToDbRequest> dbNotificationRequestCaptor;

    private UserAccountEntity requester;
    private CourtCaseEntity caseEntity;
    private CourthouseEntity courthouseEntity;

    @BeforeEach
    void setup() {
        requester = new UserAccountEntity();
        requester.setEmailAddress("test.user@example.com");
        transcriptionEntity.setCreatedBy(requester);
        Mockito.lenient().when(transcriptionEntity.getRequestedBy()).thenReturn(requester);
        Mockito.lenient().when(transcriptionEntity.getIsManualTranscription()).thenReturn(true);

        courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName("Hogwarts");

        caseEntity = new CourtCaseEntity();
        caseEntity.setId(1);
        caseEntity.setCourthouse(courthouseEntity);
        Mockito.lenient().when(transcriptionEntity.getCourtCase()).thenReturn(caseEntity);
    }

    @Test
    void notifyRequesterWithEmptyTemplateMap() {
        transcriptionNotifications.notifyRequestor(transcriptionEntity, TRANSCRIPTION_AVAILABLE.toString());

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());

        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getCaseId(), caseEntity.getId());
        assertEquals(actual.getEventId(), TRANSCRIPTION_AVAILABLE.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 1);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), requester.getEmailAddress());
    }

    @Test
    void notifyRequesterWithTemplateMap() {
        var reason = "Rejection reason";
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put(REJECTION_REASON, reason);
        transcriptionNotifications.notifyRequestor(transcriptionEntity, TRANSCRIPTION_REQUEST_REJECTED.toString(), templateParams);

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());

        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getCaseId(), caseEntity.getId());
        assertEquals(actual.getEventId(), TRANSCRIPTION_REQUEST_REJECTED.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 1);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), requester.getEmailAddress());
        assertEquals(actual.getTemplateValues().size(), 1);
        assertEquals(actual.getTemplateValues().get(REJECTION_REASON), reason);
    }

    @Test
    void notifyApprovers() {
        var approver1 = new UserAccountEntity();
        approver1.setEmailAddress("approver1@example.com");
        var approver2 = new UserAccountEntity();
        approver2.setEmailAddress("approver2@example.com");
        var approver3 = new UserAccountEntity();
        approver3.setEmailAddress("approver3@example.com");
        when(transcriptionEntity.getRequestedBy()).thenReturn(approver3);

        when(authorisationApi.getUsersWithRoleAtCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity, approver3)).thenReturn(
            List.of(approver1, approver2));

        transcriptionNotifications.notifyApprovers(transcriptionEntity);

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getCaseId(), caseEntity.getId());
        assertEquals(actual.getEventId(), COURT_MANAGER_APPROVE_TRANSCRIPT.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 2);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), approver1.getEmailAddress());
        assertEquals(actual.getUserAccountsToEmail().get(1).getEmailAddress(), approver2.getEmailAddress());
        verify(authorisationApi, times(1))
            .getUsersWithRoleAtCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity, approver3);
        verify(transcriptionEntity, times(1))
            .getRequestedBy();
    }

    @Test
    void notifyTranscriptionCompanyForCourthouse() {
        mockTranscribers();

        transcriptionNotifications.notifyTranscriptionCompanyForCourthouse(caseEntity);

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getCaseId(), caseEntity.getId());
        assertEquals(actual.getEventId(), REQUEST_TO_TRANSCRIBER.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 2);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), "transcriber1@example.com");
        assertEquals(actual.getUserAccountsToEmail().get(1).getEmailAddress(), "transcriber2@example.com");
    }

    @Test
    void notifyTranscriptionCompanyForCourthouseNoTranscribers() {
        when(authorisationApi.getUsersWithRoleAtCourthouse(SecurityRoleEnum.TRANSCRIBER, courthouseEntity)).thenReturn(List.of());
        transcriptionNotifications.notifyTranscriptionCompanyForCourthouse(caseEntity);
        verifyNoInteractions(notificationApi);
    }

    @Test
    void handleNotificationsAndAuditApproved() {
        mockTranscribers();
        var approver = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.APPROVED.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(approver, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verify(notificationApi, times(2)).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getAllValues();
        assertEquals(actual.size(), 2);
        // notification to transcribers
        assertEquals(actual.getFirst().getEventId(), REQUEST_TO_TRANSCRIBER.toString());
        assertEquals(actual.getFirst().getUserAccountsToEmail().size(), 2);
        assertEquals(actual.getFirst().getUserAccountsToEmail().getFirst().getEmailAddress(), "transcriber1@example.com");
        assertEquals(actual.getFirst().getUserAccountsToEmail().get(1).getEmailAddress(), "transcriber2@example.com");
        // notification to requester
        assertEquals(actual.get(1).getEventId(), TRANSCRIPTION_REQUEST_APPROVED.toString());
        assertEquals(actual.get(1).getUserAccountsToEmail().size(), 1);
        assertEquals(actual.get(1).getUserAccountsToEmail().getFirst().getEmailAddress(), requester.getEmailAddress());
        // audit
        verify(auditApi).record(AUTHORISE_TRANSCRIPTION, approver, caseEntity);
    }

    @Test
    void handleNotificationsAndAuditRejected() {
        var rejecter = new UserAccountEntity();
        var reason = "Rejected";
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.REJECTED.getId());
        var updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setWorkflowComment(reason);

        transcriptionNotifications.handleNotificationsAndAudit(rejecter, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        // notification to requester
        assertEquals(actual.getEventId(), TRANSCRIPTION_REQUEST_REJECTED.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 1);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), requester.getEmailAddress());
        assertEquals(actual.getTemplateValues().size(), 1);
        assertEquals(actual.getTemplateValues().get(REJECTION_REASON), reason);
        // audit
        verify(auditApi).record(REJECT_TRANSCRIPTION, rejecter, caseEntity);
    }

    @Test
    void handleNotificationsAndAuditWithTranscriber() {
        var transcriber = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(transcriber, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        // audit
        verify(auditApi).record(ACCEPT_TRANSCRIPTION, transcriber, caseEntity);
    }

    @Test
    void handleNotificationsAndAuditComplete() {
        var transcriber = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.COMPLETE.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(transcriber, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verify(notificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        // notification to requester
        assertEquals(actual.getEventId(), TRANSCRIPTION_AVAILABLE.toString());
        assertEquals(actual.getUserAccountsToEmail().size(), 1);
        assertEquals(actual.getUserAccountsToEmail().getFirst().getEmailAddress(), requester.getEmailAddress());
        // audit
        verify(auditApi).record(COMPLETE_TRANSCRIPTION, transcriber, caseEntity);
    }

    @Test
    void handleNotificationsAndAuditApprovedIsNotManual() {
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(false);
        var transcriber = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.APPROVED.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(transcriber, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verifyNoInteractions(notificationApi);
        // audit
        verify(auditApi).record(AUTHORISE_TRANSCRIPTION, transcriber, caseEntity);
    }


    @Test
    void handleNotificationsAndAuditRejectedIsNotManual() {
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(false);
        var transcriber = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.REJECTED.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(transcriber, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verifyNoInteractions(notificationApi);
        // audit
        verify(auditApi).record(REJECT_TRANSCRIPTION, transcriber, caseEntity);
    }

    @Test
    void handleNotificationsAndAuditCompleteIsNotManual() {
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(false);
        var transcriber = new UserAccountEntity();
        var transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionStatusEntity.setId(TranscriptionStatusEnum.COMPLETE.getId());
        var updateTranscription = new UpdateTranscriptionRequest();

        transcriptionNotifications.handleNotificationsAndAudit(transcriber, transcriptionEntity, transcriptionStatusEntity, updateTranscription);

        verifyNoInteractions(notificationApi);
        // audit
        verify(auditApi).record(COMPLETE_TRANSCRIPTION, transcriber, caseEntity);
    }

    private void mockTranscribers() {
        var transcriber1 = new UserAccountEntity();
        transcriber1.setEmailAddress("transcriber1@example.com");
        var transcriber2 = new UserAccountEntity();
        transcriber2.setEmailAddress("transcriber2@example.com");
        when(authorisationApi.getUsersWithRoleAtCourthouse(SecurityRoleEnum.TRANSCRIBER, courthouseEntity)).thenReturn(List.of(transcriber1, transcriber2));
    }
}
