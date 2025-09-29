package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
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

import static uk.gov.hmcts.darts.audit.api.AuditActivity.ACCEPT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AUTHORISE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.COMPLETE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REJECT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UNFULFILLED_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REJECTION_REASON;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.UNFULFILLED_REASON;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.COURT_MANAGER_APPROVE_TRANSCRIPT;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.REQUEST_TO_TRANSCRIBER;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_AVAILABLE;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_APPROVED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_REJECTED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_UNFULFILLED;

@RequiredArgsConstructor
@Slf4j
@Service
public class TranscriptionNotifications {

    private final AuthorisationApi authorisationApi;
    private final NotificationApi notificationApi;
    private final AuditApi auditApi;


    @SuppressWarnings({"java:S131", "checkstyle:MissingSwitchDefault", "PMD.CyclomaticComplexity"})
    public void handleNotificationsAndAudit(UserAccountEntity userAccountEntity,
                                            TranscriptionEntity transcriptionEntity,
                                            TranscriptionStatusEntity transcriptionStatusEntity, UpdateTranscriptionRequest updateTranscription) {
        TranscriptionStatusEnum newStatusEnum = TranscriptionStatusEnum.fromId(transcriptionStatusEntity.getId());

        final var courtCaseEntity = transcriptionEntity.getCourtCase();
        switch (newStatusEnum) {
            case APPROVED -> {
                notifyTranscriptionCompanyForCourthouse(courtCaseEntity);
                if (Boolean.TRUE.equals(transcriptionEntity.getIsManualTranscription())) {
                    notifyRequestor(transcriptionEntity, TRANSCRIPTION_REQUEST_APPROVED.toString());
                }
                auditApi.record(
                    AUTHORISE_TRANSCRIPTION,
                    userAccountEntity,
                    courtCaseEntity
                );
            }
            case REJECTED -> {
                Map<String, String> templateParams = new HashMap<>();
                templateParams.put(REJECTION_REASON, updateTranscription.getWorkflowComment());

                if (Boolean.TRUE.equals(transcriptionEntity.getIsManualTranscription())) {
                    notifyRequestor(transcriptionEntity, TRANSCRIPTION_REQUEST_REJECTED.toString(), templateParams);
                }
                auditApi.record(REJECT_TRANSCRIPTION, userAccountEntity, courtCaseEntity);
            }
            case WITH_TRANSCRIBER -> auditApi.record(ACCEPT_TRANSCRIPTION, userAccountEntity, courtCaseEntity);
            case COMPLETE -> {
                if (Boolean.TRUE.equals(transcriptionEntity.getIsManualTranscription())) {
                    notifyRequestor(transcriptionEntity, TRANSCRIPTION_AVAILABLE.toString());
                }
                auditApi.record(COMPLETE_TRANSCRIPTION, userAccountEntity, courtCaseEntity);
            }
            case UNFULFILLED -> {
                Map<String, String> templateParams = new HashMap<>();
                templateParams.put(UNFULFILLED_REASON, updateTranscription.getWorkflowComment());

                if (Boolean.TRUE.equals(transcriptionEntity.getIsManualTranscription())) {
                    notifyRequestor(transcriptionEntity, TRANSCRIPTION_REQUEST_UNFULFILLED.toString(), templateParams);
                }
                auditApi.record(UNFULFILLED_TRANSCRIPTION, userAccountEntity, courtCaseEntity);
            }
            default -> {
                // Do nothing for unmatched status
            }
        }
    }


    public void notifyRequestor(TranscriptionEntity transcription, String templateName, Map<String, String> templateParams) {
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(templateName)
            .userAccountsToEmail(List.of(transcription.getRequestedBy()))
            .caseId(transcription.getCourtCase().getId())
            .templateValues(templateParams)
            .build();
        notificationApi.scheduleNotification(request);
    }

    public void notifyRequestor(TranscriptionEntity transcription, String templateName) {
        notifyRequestor(transcription, templateName, new HashMap<>());
    }

    public void notifyApprovers(TranscriptionEntity transcription) {
        List<UserAccountEntity> usersToNotify = authorisationApi.getUsersWithRoleAtCourthouse(
            SecurityRoleEnum.APPROVER,
            transcription.getCourtCase().getCourthouse(),
            transcription.getRequestedBy()
        );
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(COURT_MANAGER_APPROVE_TRANSCRIPT.toString())
            .userAccountsToEmail(usersToNotify)
            .caseId(transcription.getCourtCase().getId())
            .build();
        notificationApi.scheduleNotification(request);
    }

    public void notifyTranscriptionCompanyForCourthouse(CourtCaseEntity courtCase) {
        //find users to notify
        List<UserAccountEntity> usersToNotify = authorisationApi.getUsersWithRoleAtCourthouse(
            TRANSCRIBER,
            courtCase.getCourthouse()
        );
        if (usersToNotify.isEmpty()) {
            log.error(
                "No Transcription company users could be found for courthouse {}",
                courtCase.getCourthouse().getCourthouseName()
            );
            return;
        }

        //schedule notification
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(REQUEST_TO_TRANSCRIBER.toString())
            .userAccountsToEmail(usersToNotify)
            .caseId(courtCase.getId())
            .build();
        notificationApi.scheduleNotification(request);
    }


}
