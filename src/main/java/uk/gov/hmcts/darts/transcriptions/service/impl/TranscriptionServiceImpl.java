package uk.gov.hmcts.darts.transcriptions.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.REQUEST_TO_TRANSCRIBER;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.TRANSCRIPTION_REQUEST_APPROVED;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.TRANSCRIPTION_REQUEST_REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_WORKFLOW_COMMENT;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_WORKFLOW_ACTION_INVALID;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionServiceImpl implements TranscriptionService {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final AuthorisationApi authorisationApi;
    private final NotificationApi notificationApi;

    private final CaseService caseService;
    private final HearingsService hearingsService;

    private final UserIdentity userIdentity;

    private final WorkflowValidator workflowValidator;


    @Transactional
    @Override
    public void saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails) {

        UserAccountEntity userAccount = getUserAccount();
        TranscriptionStatusEntity transcriptionStatus = getTranscriptionStatusById(REQUESTED.getId());

        TranscriptionEntity transcription = saveTranscription(
            userAccount,
            transcriptionRequestDetails,
            transcriptionStatus,
            getTranscriptionTypeById(transcriptionRequestDetails.getTranscriptionTypeId()),
            getTranscriptionUrgencyById(transcriptionRequestDetails.getUrgencyId())
        );

        saveTranscriptionWorkflow(
            userAccount,
            transcription,
            transcriptionStatus,
            transcriptionRequestDetails.getComment()
        );
    }

    @Override
    @Transactional
    public UpdateTranscriptionResponse updateTranscription(Integer transcriptionId,
                                                           UpdateTranscription updateTranscription) {

        TranscriptionEntity transcription = transcriptionRepository.findById(transcriptionId)
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));

        validateUpdateTranscription(transcription, updateTranscription);

        TranscriptionStatusEntity transcriptionStatusEntity = getTranscriptionStatusById(updateTranscription.getTranscriptionStatusId());
        transcription.setTranscriptionStatus(transcriptionStatusEntity);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = saveTranscriptionWorkflow(
            getUserAccount(),
            transcription,
            transcriptionStatusEntity,
            updateTranscription.getWorkflowComment()
        );
        transcription.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);

        UpdateTranscriptionResponse updateTranscriptionResponse = new UpdateTranscriptionResponse();
        updateTranscriptionResponse.setTranscriptionWorkflowId(transcriptionWorkflowEntity.getId());

        TranscriptionStatusEnum newStatusEnum = TranscriptionStatusEnum.fromId(transcriptionStatusEntity.getId());

        if (newStatusEnum == APPROVED) {
            notifyTranscriptionCompanyForCourthouse(transcription.getCourtCase());
            notifyRequestor(transcription, TRANSCRIPTION_REQUEST_APPROVED);
        } else if (newStatusEnum == REJECTED) {
            notifyRequestor(transcription, TRANSCRIPTION_REQUEST_REJECTED);
        }
        return updateTranscriptionResponse;
    }

    private void validateUpdateTranscription(TranscriptionEntity transcription,
                                             UpdateTranscription updateTranscription) {

        TranscriptionStatusEnum desiredTargetTranscriptionStatus = TranscriptionStatusEnum.fromId(updateTranscription.getTranscriptionStatusId());

        if (!workflowValidator.validateChangeToWorkflowStatus(
            TranscriptionTypeEnum.fromId(transcription.getTranscriptionType().getId()),
            TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId()),
            desiredTargetTranscriptionStatus
        )) {
            throw new DartsApiException(TRANSCRIPTION_WORKFLOW_ACTION_INVALID);
        }

        if (REJECTED.equals(desiredTargetTranscriptionStatus)
            && StringUtils.isBlank(updateTranscription.getWorkflowComment())) {
            throw new DartsApiException(BAD_REQUEST_WORKFLOW_COMMENT);
        }
    }

    private void notifyRequestor(TranscriptionEntity transcription, String templateName) {
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(templateName)
            .userAccountsToEmail(List.of(transcription.getCreatedBy()))
            .caseId(transcription.getCourtCase().getId())
            .build();
        notificationApi.scheduleNotification(request);
    }

    private void notifyTranscriptionCompanyForCourthouse(CourtCaseEntity courtCase) {
        //find users to notify
        List<UserAccountEntity> usersToNotify = authorisationApi.getUsersWithRoleAtCourthouse(
            SecurityRoleEnum.TRANSCRIBER,
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
            .eventId(REQUEST_TO_TRANSCRIBER)
            .userAccountsToEmail(usersToNotify)
            .caseId(courtCase.getId())
            .build();
        notificationApi.scheduleNotification(request);
    }

    private TranscriptionEntity saveTranscription(UserAccountEntity userAccount,
                                                  TranscriptionRequestDetails transcriptionRequestDetails,
                                                  TranscriptionStatusEntity transcriptionStatus,
                                                  TranscriptionTypeEntity transcriptionType,
                                                  TranscriptionUrgencyEntity transcriptionUrgency) {

        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setStart(transcriptionRequestDetails.getStartDateTime());
        transcription.setEnd(transcriptionRequestDetails.getEndDateTime());
        transcription.setCreatedBy(userAccount);
        transcription.setLastModifiedBy(userAccount);

        if (nonNull(transcriptionRequestDetails.getCaseId())) {
            transcription.setCourtCase(caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId()));
        }

        if (nonNull(transcriptionRequestDetails.getHearingId())) {
            HearingEntity hearing = hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
            transcription.setHearing(hearing);
            if (isNull(transcription.getCourtCase())) {
                transcription.setCourtCase(hearing.getCourtCase());
            }
        }

        return transcriptionRepository.saveAndFlush(transcription);
    }

    private TranscriptionWorkflowEntity saveTranscriptionWorkflow(UserAccountEntity userAccount,
                                                                  TranscriptionEntity transcription,
                                                                  TranscriptionStatusEntity transcriptionStatus,
                                                                  String workflowComment) {

        TranscriptionWorkflowEntity transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(transcription);
        transcriptionWorkflow.setTranscriptionStatus(transcriptionStatus);
        transcriptionWorkflow.setWorkflowActor(userAccount);
        transcriptionWorkflow.setWorkflowTimestamp(OffsetDateTime.now(UTC));
        transcriptionWorkflow.setWorkflowComment(workflowComment);
        transcriptionWorkflow.setCreatedBy(userAccount);
        transcriptionWorkflow.setLastModifiedBy(userAccount);

        return transcriptionWorkflowRepository.saveAndFlush(transcriptionWorkflow);
    }

    private UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }

    private TranscriptionUrgencyEntity getTranscriptionUrgencyById(Integer urgencyId) {
        return transcriptionUrgencyRepository.getReferenceById(urgencyId);
    }

    private TranscriptionStatusEntity getTranscriptionStatusById(Integer transcriptionStatusId) {
        return transcriptionStatusRepository.getReferenceById(transcriptionStatusId);
    }

    private TranscriptionTypeEntity getTranscriptionTypeById(Integer transcriptionTypeId) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeId);
    }

}
