package uk.gov.hmcts.darts.transcriptions.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptFileValidator;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audit.enums.AuditActivityEnum.IMPORT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.enums.AuditActivityEnum.REQUEST_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.REQUEST_TO_TRANSCRIBER;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.TRANSCRIPTION_REQUEST_APPROVED;
import static uk.gov.hmcts.darts.notification.NotificationConstants.TemplateNames.TRANSCRIPTION_REQUEST_REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_WORKFLOW_COMMENT;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_ATTACH_TRANSCRIPT;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_WORKFLOW_ACTION_INVALID;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionServiceImpl implements TranscriptionService {

    private static final String AUTOMATICALLY_CLOSED_TRANSCRIPTION = "Automatically closed transcription";

    private final TranscriptionConfigurationProperties transcriptionConfigurationProperties;

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final TranscriptionCommentRepository transcriptionCommentRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final AuthorisationApi authorisationApi;
    private final NotificationApi notificationApi;
    private final DataManagementApi dataManagementApi;

    private final CaseService caseService;
    private final HearingsService hearingsService;
    private final AuditService auditService;

    private final UserIdentity userIdentity;

    private final WorkflowValidator workflowValidator;
    private final TranscriptFileValidator transcriptFileValidator;

    @Override
    @Transactional
    public RequestTranscriptionResponse saveTranscriptionRequest(
        TranscriptionRequestDetails transcriptionRequestDetails) {

        UserAccountEntity userAccount = getUserAccount();
        TranscriptionStatusEntity transcriptionStatus = getTranscriptionStatusById(REQUESTED.getId());

        TranscriptionEntity transcription = saveTranscription(
            userAccount,
            transcriptionRequestDetails,
            transcriptionStatus,
            getTranscriptionTypeById(transcriptionRequestDetails.getTranscriptionTypeId()),
            getTranscriptionUrgencyById(transcriptionRequestDetails.getUrgencyId())
        );

        transcription.getTranscriptionWorkflowEntities().add(
            saveTranscriptionWorkflow(
                userAccount,
                transcription,
                transcriptionStatus,
                transcriptionRequestDetails.getComment()
            ));

        if (!workflowValidator.isAutomatedTranscription(TranscriptionTypeEnum.fromId(
            transcription.getTranscriptionType().getId()))) {
            transcriptionStatus = getTranscriptionStatusById(AWAITING_AUTHORISATION.getId());

            transcription.getTranscriptionWorkflowEntities().add(
                saveTranscriptionWorkflow(
                    userAccount,
                    transcription,
                    transcriptionStatus,
                    null
                ));
        }

        auditService.recordAudit(REQUEST_TRANSCRIPTION, userAccount, transcription.getCourtCase());

        RequestTranscriptionResponse requestTranscriptionResponse = new RequestTranscriptionResponse();
        requestTranscriptionResponse.setTranscriptionId(transcription.getId());
        return requestTranscriptionResponse;
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
        transcription.setStartTime(transcriptionRequestDetails.getStartDateTime());
        transcription.setEndTime(transcriptionRequestDetails.getEndDateTime());
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

        TranscriptionWorkflowEntity savedTranscriptionWorkFlow = transcriptionWorkflowRepository.saveAndFlush(
            transcriptionWorkflow);

        if (!StringUtils.isBlank(workflowComment)) {
            createAndSaveComment(userAccount, workflowComment, savedTranscriptionWorkFlow, transcription);
        }
        return savedTranscriptionWorkFlow;
    }

    private void createAndSaveComment(UserAccountEntity userAccount, String workflowComment,
                                      TranscriptionWorkflowEntity savedTranscriptionWorkFlow,
                                      TranscriptionEntity transcription) {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment(workflowComment);
        transcriptionCommentEntity.setTranscriptionWorkflowId(savedTranscriptionWorkFlow.getId());
        transcriptionCommentEntity.setTranscription(transcription);
        transcriptionCommentEntity.setLastModifiedBy(userAccount);
        transcriptionCommentEntity.setCreatedBy(userAccount);
        transcriptionCommentRepository.saveAndFlush(transcriptionCommentEntity);
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

    @Override
    @Transactional
    public void closeTranscriptions() {
        try {
            List<TranscriptionStatusEntity> finishedTranscriptionStatuses = getFinishedTranscriptionStatuses();
            OffsetDateTime lastCreatedDateTime = OffsetDateTime.now()
                .minus(transcriptionConfigurationProperties.getMaxCreatedByDuration());
            List<TranscriptionEntity> transcriptionsToBeClosed =
                transcriptionRepository.findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
                    finishedTranscriptionStatuses,
                    lastCreatedDateTime
                );
            if (isNull(transcriptionsToBeClosed) || transcriptionsToBeClosed.isEmpty()) {
                log.info("No transcriptions to be closed off");
            } else {
                log.info("Number of transcriptions to be closed off: {}", transcriptionsToBeClosed.size());
                for (TranscriptionEntity transcriptionToBeClosed : transcriptionsToBeClosed) {
                    closeTranscription(transcriptionToBeClosed.getId(), AUTOMATICALLY_CLOSED_TRANSCRIPTION);
                }
            }
        } catch (Exception e) {
            log.error("Unable to close transcriptions {}", e.getMessage());
        }
    }

    private void closeTranscription(Integer transcriptionId, String transcriptionComment) {
        try {
            UpdateTranscription updateTranscription = new UpdateTranscription();
            updateTranscription.setTranscriptionStatusId(TranscriptionStatusEnum.CLOSED.getId());
            updateTranscription.setWorkflowComment(transcriptionComment);
            updateTranscription(transcriptionId, updateTranscription);
            log.info("Closed off transcription {}", transcriptionId);
        } catch (Exception e) {
            log.error("Unable to close transcription {} - {}", transcriptionId, e.getMessage());
        }
    }

    @Override
    public List<TranscriptionTypeResponse> getTranscriptionTypes() {
        return TranscriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeRepository.findAll());
    }

    @Override
    public List<TranscriptionUrgencyResponse> getTranscriptionUrgencies() {
        return TranscriptionResponseMapper.mapToTranscriptionUrgencyResponses(transcriptionUrgencyRepository.findAll());
    }

    @Override
    @Transactional
    public AttachTranscriptResponse attachTranscript(Integer transcriptionId,
                                                     MultipartFile transcript) {

        transcriptFileValidator.validate(transcript);

        final var updateTranscription = updateTranscription(transcriptionId, new UpdateTranscription(COMPLETE.getId()));

        final UUID externalLocation;
        try {
            externalLocation = dataManagementApi.saveBlobDataToInboundContainer(
                BinaryData.fromStream(transcript.getInputStream()));
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_ATTACH_TRANSCRIPT, e);
        }

        var userAccountEntity = getUserAccount();

        TranscriptionEntity transcriptionEntity = transcriptionRepository.getReferenceById(transcriptionId);
        var transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName(transcript.getOriginalFilename());
        transcriptionDocumentEntity.setFileType(transcript.getContentType());
        transcriptionDocumentEntity.setFileSize((int) transcript.getSize());
        transcriptionDocumentEntity.setUploadedBy(userAccountEntity);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = saveExternalObjectDirectory(
            externalLocation, userAccountEntity, transcriptionDocumentEntity);

        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryEntity);
        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);

        auditService.recordAudit(IMPORT_TRANSCRIPTION, userAccountEntity, transcriptionEntity.getCourtCase());

        var attachTranscriptResponse = new AttachTranscriptResponse();
        attachTranscriptResponse.setTranscriptionDocumentId(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()
                                                                .getId());
        attachTranscriptResponse.setTranscriptionWorkflowId(updateTranscription.getTranscriptionWorkflowId());

        return attachTranscriptResponse;
    }

    private ExternalObjectDirectoryEntity saveExternalObjectDirectory(UUID externalLocation,
                                                                      UserAccountEntity userAccountEntity,
                                                                      TranscriptionDocumentEntity transcriptionDocumentEntity) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        externalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.STORED.getId()));
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(INBOUND.getId()));
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
        return externalObjectDirectoryEntity;
    }

    private List<TranscriptionStatusEntity> getFinishedTranscriptionStatuses() {
        List<TranscriptionStatusEntity> transcriptionStatuses = new ArrayList<>();
        transcriptionStatuses.add(getTranscriptionStatusById(TranscriptionStatusEnum.CLOSED.getId()));
        transcriptionStatuses.add(getTranscriptionStatusById(TranscriptionStatusEnum.COMPLETE.getId()));
        transcriptionStatuses.add(getTranscriptionStatusById(TranscriptionStatusEnum.REJECTED.getId()));
        return transcriptionStatuses;
    }

}
