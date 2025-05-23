package uk.gov.hmcts.darts.transcriptions.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.exception.PartialFailureException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.component.TranscriberTranscriptsQuery;
import uk.gov.hmcts.darts.transcriptions.component.YourTranscriptsQuery;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.helper.UpdateTranscriptionEntityHelper;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionStatus;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTranscriberCountsResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsUpdateValidator;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptFileValidator;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.IMPORT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REQUEST_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.transcriptions.auditing.TranscriptionUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_REQUESTER_IS_SAME_AS_APPROVER;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_WORKFLOW_COMMENT;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_UPLOAD_TRANSCRIPT;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_WORKFLOW_ACTION_INVALID;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.USER_NOT_TRANSCRIBER;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.CouplingBetweenObjects",
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class TranscriptionServiceImpl implements TranscriptionService {

    public static final int INITIAL_VERIFICATION_ATTEMPTS = 1;

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final TranscriptionCommentRepository transcriptionCommentRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final UserAccountRepository userAccountRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;

    private final TranscriptionNotifications transcriptionNotifications;
    private final DataManagementApi dataManagementApi;

    private final HearingsService hearingsService;
    private final AuditApi auditApi;

    private final UserIdentity userIdentity;

    private final WorkflowValidator workflowValidator;
    private final TranscriptFileValidator transcriptFileValidator;
    private final FileContentChecksum fileContentChecksum;

    private final YourTranscriptsQuery yourTranscriptsQuery;
    private final DuplicateRequestDetector duplicateRequestDetector;
    private final TranscriberTranscriptsQuery transcriberTranscriptsQuery;
    private final List<TranscriptionsUpdateValidator> updateTranscriptionsValidator;
    private final TranscriptionResponseMapper transcriptionResponseMapper;
    private final TranscriptionDownloader transcriptionDownloader;
    private final TranscriptionLinkedCaseRepository transcriptionLinkedCaseRepository;

    @Value("${darts.manual-deletion.enabled:false}")
    @Getter(AccessLevel.PACKAGE)
    private boolean manualDeletionEnabled;

    private static final String OWNER_DISABLED_COMMENT_MESSAGE = "Owner was disabled";

    @Override
    @Transactional
    public RequestTranscriptionResponse saveTranscriptionRequest(
        TranscriptionRequestDetails transcriptionRequestDetails, boolean isManual) {

        UserAccountEntity userAccount = getUserAccount();
        TranscriptionStatusEntity transcriptionStatus = getTranscriptionStatusById(REQUESTED.getId());

        duplicateRequestDetector.checkForDuplicate(transcriptionRequestDetails, isManual);

        TranscriptionEntity transcription = saveTranscription(
            userAccount,
            transcriptionRequestDetails,
            transcriptionStatus,
            getTranscriptionTypeById(transcriptionRequestDetails.getTranscriptionTypeId()),
            getTranscriptionUrgencyById(transcriptionRequestDetails.getTranscriptionUrgencyId()),
            isManual
        );

        transcriptionLinkedCaseRepository.save(
            TranscriptionLinkedCaseEntity.builder()
                .transcription(transcription)
                .courtCase(transcription.getCourtCase())
                .build());

        transcription.getTranscriptionWorkflowEntities().add(
            saveTranscriptionWorkflow(
                userAccount,
                transcription,
                transcriptionStatus,
                transcriptionRequestDetails.getComment()
            ));

        moveTranscriptionRequestedToAwaitingAuthorisation(transcription, transcriptionStatus);

        auditApi.record(REQUEST_TRANSCRIPTION, userAccount, transcription.getCourtCase());

        RequestTranscriptionResponse requestTranscriptionResponse = new RequestTranscriptionResponse();
        requestTranscriptionResponse.setTranscriptionId(transcription.getId());
        return requestTranscriptionResponse;
    }

    @Override
    @Transactional
    public UpdateTranscriptionResponse updateTranscription(Long transcriptionId,
                                                           UpdateTranscriptionRequest updateTranscription, Boolean allowSelfApprovalOrRejection) {
        final var userAccountEntity = getUserAccount();
        final var transcriptionEntity = transcriptionRepository.findById(transcriptionId)
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));

        validateUpdateTranscription(transcriptionEntity, updateTranscription, allowSelfApprovalOrRejection, false);

        final var transcriptionStatusEntity = getTranscriptionStatusById(updateTranscription.getTranscriptionStatusId());
        transcriptionEntity.setTranscriptionStatus(transcriptionStatusEntity);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = saveTranscriptionWorkflow(
            getUserAccount(),
            transcriptionEntity,
            transcriptionStatusEntity,
            updateTranscription.getWorkflowComment()
        );
        transcriptionEntity.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);

        UpdateTranscriptionResponse updateTranscriptionResponse = new UpdateTranscriptionResponse();
        updateTranscriptionResponse.setTranscriptionWorkflowId(transcriptionWorkflowEntity.getId());

        transcriptionNotifications.handleNotificationsAndAudit(userAccountEntity, transcriptionEntity, transcriptionStatusEntity, updateTranscription);
        return updateTranscriptionResponse;
    }

    @Override
    @Transactional
    public UpdateTranscriptionAdminResponse updateTranscriptionAdmin(Long transcriptionId,
                                                                     UpdateTranscriptionRequest updateTranscription, Boolean allowSelfApprovalOrRejection) {
        final var userAccountEntity = getUserAccount();
        final var transcriptionEntity = transcriptionRepository.findById(transcriptionId)
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));

        validateUpdateTranscription(transcriptionEntity, updateTranscription, allowSelfApprovalOrRejection, true);

        var auditActivityProvider = auditActivitiesFor(transcriptionEntity, updateTranscription);

        var transcriptionStatusEntity = getTranscriptionStatusById(updateTranscription.getTranscriptionStatusId());

        transcriptionEntity.setTranscriptionStatus(transcriptionStatusEntity);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = saveTranscriptionWorkflow(
            getUserAccount(),
            transcriptionEntity,
            transcriptionStatusEntity,
            updateTranscription.getWorkflowComment()
        );

        auditApi.recordAll(auditActivityProvider, transcriptionEntity.getCourtCase());

        transcriptionEntity.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);

        if (REQUESTED.getId().equals(transcriptionStatusEntity.getId())) {
            transcriptionStatusEntity = moveTranscriptionRequestedToAwaitingAuthorisation(transcriptionEntity, transcriptionStatusEntity);
        }
        UpdateTranscriptionAdminResponse updateTranscriptionResponse = new UpdateTranscriptionAdminResponse();
        updateTranscriptionResponse.setTranscriptionId(transcriptionEntity.getId());
        updateTranscriptionResponse.setTranscriptionStatusId(transcriptionEntity.getTranscriptionStatus().getId());

        transcriptionNotifications.handleNotificationsAndAudit(userAccountEntity, transcriptionEntity, transcriptionStatusEntity, updateTranscription);
        return updateTranscriptionResponse;
    }

    private TranscriptionStatusEntity moveTranscriptionRequestedToAwaitingAuthorisation(TranscriptionEntity transcriptionEntity,
                                                                                        TranscriptionStatusEntity transcriptionStatusEntity) {
        if (!transcriptionEntity.getIsManualTranscription()) {
            return transcriptionStatusEntity;
        }


        TranscriptionStatusEntity awaitingAuthTranscriptionStatusEntity = getTranscriptionStatusById(AWAITING_AUTHORISATION.getId());
        transcriptionEntity.setTranscriptionStatus(awaitingAuthTranscriptionStatusEntity);

        transcriptionEntity.getTranscriptionWorkflowEntities().add(
            saveTranscriptionWorkflow(
                getUserAccount(),
                transcriptionEntity,
                awaitingAuthTranscriptionStatusEntity,
                null
            ));

        transcriptionNotifications.notifyApprovers(transcriptionEntity);

        return awaitingAuthTranscriptionStatusEntity;
    }


    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    void validateUpdateTranscription(TranscriptionEntity transcription,
                                     UpdateTranscriptionRequest updateTranscription, boolean allowSelfApprovalOrRejection, boolean isAdmin) {

        TranscriptionStatusEnum desiredTargetTranscriptionStatus = TranscriptionStatusEnum.fromId(updateTranscription.getTranscriptionStatusId());
        UserAccountEntity transcriptionUser = userAccountRepository.getReferenceById(transcription.getCreatedById());
        if (!allowSelfApprovalOrRejection && getUserAccount().getId().equals(transcriptionUser.getId())
            && (desiredTargetTranscriptionStatus.equals(REJECTED) || desiredTargetTranscriptionStatus.equals(APPROVED))) {
            throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_REQUESTER_IS_SAME_AS_APPROVER);
        }

        if (!workflowValidator.validateChangeToWorkflowStatus(
            transcription.getIsManualTranscription(),
            TranscriptionTypeEnum.fromId(transcription.getTranscriptionType().getId()),
            TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId()),
            desiredTargetTranscriptionStatus,
            isAdmin)) {

            throw new DartsApiException(TRANSCRIPTION_WORKFLOW_ACTION_INVALID);
        }

        if (REJECTED.equals(desiredTargetTranscriptionStatus)
            && StringUtils.isBlank(updateTranscription.getWorkflowComment())) {
            throw new DartsApiException(BAD_REQUEST_WORKFLOW_COMMENT);
        }
    }

    private TranscriptionEntity saveTranscription(UserAccountEntity userAccount,
                                                  TranscriptionRequestDetails transcriptionRequestDetails,
                                                  TranscriptionStatusEntity transcriptionStatus,
                                                  TranscriptionTypeEntity transcriptionType,
                                                  TranscriptionUrgencyEntity transcriptionUrgency, boolean isManual) {

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
        transcription.setIsManualTranscription(isManual);
        transcription.setHideRequestFromRequestor(false);
        transcription.setIsCurrent(true);
        transcription.setRequestedBy(userAccount);

        if (nonNull(transcriptionRequestDetails.getHearingId())) {
            HearingEntity hearing = hearingsService.getHearingByIdWithValidation(transcriptionRequestDetails.getHearingId());
            transcription.addHearing(hearing);
        }

        return transcriptionRepository.saveAndFlush(transcription);
    }

    @Override
    public TranscriptionWorkflowEntity saveTranscriptionWorkflow(UserAccountEntity userAccount,
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

        if (StringUtils.isNotBlank(workflowComment)) {
            createAndSaveComment(userAccount, workflowComment, savedTranscriptionWorkFlow, transcription);
        }
        return savedTranscriptionWorkFlow;
    }

    private void createAndSaveComment(UserAccountEntity userAccount, String workflowComment,
                                      TranscriptionWorkflowEntity savedTranscriptionWorkFlow,
                                      TranscriptionEntity transcription) {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment(workflowComment);
        transcriptionCommentEntity.setTranscriptionWorkflow(savedTranscriptionWorkFlow);
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
    @SuppressWarnings("java:S6809")
    public void closeTranscription(Long transcriptionId, String transcriptionComment) {
        try {
            UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
            updateTranscription.setTranscriptionStatusId(CLOSED.getId());
            updateTranscription.setWorkflowComment(transcriptionComment);
            updateTranscription(transcriptionId, updateTranscription, false);
            log.debug("Closed off transcription {}", transcriptionId);
        } catch (Exception e) {
            log.error("Unable to close transcription {}", transcriptionId, e);
        }
    }

    @Override
    public List<TranscriptionTypeResponse> getTranscriptionTypes() {
        return transcriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeRepository.findAll());
    }

    @Override
    public List<TranscriptionUrgencyResponse> getTranscriptionUrgenciesByDisplayState() {
        return transcriptionResponseMapper.mapToTranscriptionUrgencyResponses(transcriptionUrgencyRepository.findAllByDisplayStateTrue());
    }

    @Override
    @Transactional
    @SuppressWarnings("java:S6809")
    public AttachTranscriptResponse attachTranscript(Long transcriptionId, MultipartFile transcript) {

        transcriptFileValidator.validate(transcript);

        final var updateTranscription = updateTranscription(transcriptionId, new UpdateTranscriptionRequest(COMPLETE.getId(), null), false);

        final BlobClient inboundBlobCLient;
        final BlobClient unstructuredBlobClient;
        final String checksum;

        Map<String, String> metadata = new HashMap<>();
        metadata.put(TRANSCRIPTION_ID, String.valueOf(transcriptionId));

        try {
            BinaryData binaryData = BinaryData.fromStream(transcript.getInputStream());
            checksum = fileContentChecksum.calculate(binaryData.toBytes());
            inboundBlobCLient = dataManagementApi.saveBlobDataToContainer(binaryData, DatastoreContainerType.INBOUND, metadata);
            unstructuredBlobClient = dataManagementApi.saveBlobDataToContainer(binaryData, DatastoreContainerType.UNSTRUCTURED, metadata);
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_TRANSCRIPT, e);
        }

        final var userAccountEntity = getUserAccount();
        final var transcriptionEntity = transcriptionRepository.getReferenceById(transcriptionId);

        final var transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName(transcript.getOriginalFilename());
        transcriptionDocumentEntity.setFileType(transcript.getContentType());
        transcriptionDocumentEntity.setFileSize((int) transcript.getSize());
        transcriptionDocumentEntity.setChecksum(checksum);
        transcriptionDocumentEntity.setUploadedBy(userAccountEntity);
        transcriptionDocumentEntity.setLastModifiedBy(userAccountEntity);
        transcriptionDocumentRepository.save(transcriptionDocumentEntity);

        final var externalObjectDirectoryInboundEntity = saveExternalObjectDirectory(
            inboundBlobCLient.getBlobName(), checksum, userAccountEntity, transcriptionDocumentEntity, INBOUND);
        final var externalObjectDirectoryUnstructuredEntity = saveExternalObjectDirectory(
            unstructuredBlobClient.getBlobName(), checksum, userAccountEntity, transcriptionDocumentEntity, UNSTRUCTURED);

        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryInboundEntity);
        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryUnstructuredEntity);
        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);

        auditApi.record(IMPORT_TRANSCRIPTION, userAccountEntity, transcriptionEntity.getCourtCase());

        var attachTranscriptResponse = new AttachTranscriptResponse();
        attachTranscriptResponse.setTranscriptionDocumentId(transcriptionDocumentEntity.getId());
        attachTranscriptResponse.setTranscriptionWorkflowId(updateTranscription.getTranscriptionWorkflowId());

        return attachTranscriptResponse;
    }

    @Override
    public DownloadTranscriptResponse downloadTranscript(Long transcriptionId) {
        return transcriptionDownloader.downloadTranscript(transcriptionId);
    }

    @Override
    public GetYourTranscriptsResponse getYourTranscripts(Integer userId, Boolean includeHiddenFromRequester) {
        final var getYourTranscriptsResponse = new GetYourTranscriptsResponse();
        getYourTranscriptsResponse.setRequesterTranscriptions(yourTranscriptsQuery.getRequesterTranscriptions(userId, includeHiddenFromRequester));
        getYourTranscriptsResponse.setApproverTranscriptions(yourTranscriptsQuery.getApproverTranscriptions(userId));
        return getYourTranscriptsResponse;
    }

    @Override
    public TranscriptionTranscriberCountsResponse getTranscriptionTranscriberCounts(Integer userId) {

        Optional<UserAccountEntity> user = userAccountRepository.findByRoleAndUserId(TRANSCRIBER.getId(), userId);
        if (user.isEmpty()) {
            throw new DartsApiException(USER_NOT_TRANSCRIBER);
        }

        List<Integer> courthouseIds = transcriberTranscriptsQuery.getAuthorisedCourthouses(userId, TRANSCRIBER.getId());

        Integer numUnassigned = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(courthouseIds, APPROVED.getId(), userId);
        Integer numAssigned = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(courthouseIds, WITH_TRANSCRIBER.getId(), userId);

        final var getTranscriptionTranscriberCounts = new TranscriptionTranscriberCountsResponse();
        getTranscriptionTranscriberCounts.setAssigned(numAssigned);
        getTranscriptionTranscriberCounts.setUnassigned(numUnassigned);
        return getTranscriptionTranscriberCounts;
    }

    @Override
    public List<TranscriberViewSummary> getTranscriberTranscripts(Integer userId, Boolean assigned) {
        List<TranscriberViewSummary> result;
        if (TRUE.equals(assigned)) {
            result = transcriberTranscriptsQuery.getTranscriberTranscriptions(userId);
        } else {
            result = transcriberTranscriptsQuery.getTranscriptRequests(userId);
        }
        return result;
    }

    @SuppressWarnings({"java:S2259"})
    @Override
    public List<GetTranscriptionWorkflowsResponse> getTranscriptionWorkflows(Long transcriptionId, Boolean isCurrent) {
        var transcription = transcriptionRepository.findById(transcriptionId);

        if (transcription.isEmpty()) {
            return Collections.emptyList();
        }

        var transcriptionWorkflows = transcriptionWorkflowRepository.findByTranscriptionOrderByWorkflowTimestampDesc(transcription.get());

        if (nonNull(isCurrent) && TRUE.equals(isCurrent)) {
            List<TranscriptionWorkflowEntity> workflow =
                transcriptionWorkflows.isEmpty() ? Collections.emptyList() : List.of(transcriptionWorkflows.getFirst());
            return transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(workflow, Collections.emptyList());
        }

        // migrated transcription comments are not associated to a transcription workflow
        List<TranscriptionCommentEntity> migratedTranscriptionComments =
            transcriptionCommentRepository.getByTranscriptionAndTranscriptionWorkflowIsNull(transcription.get());

        return transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(transcriptionWorkflows, migratedTranscriptionComments);
    }

    private ExternalObjectDirectoryEntity saveExternalObjectDirectory(String externalLocation,
                                                                      String checksum,
                                                                      UserAccountEntity userAccountEntity,
                                                                      TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                      ExternalLocationTypeEnum externalLocationTypeEnum) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        externalObjectDirectoryEntity.setStatus(objectRecordStatusRepository.getReferenceById(
            ObjectRecordStatusEnum.STORED.getId()));
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId()));
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(checksum);
        externalObjectDirectoryEntity.setVerificationAttempts(INITIAL_VERIFICATION_ATTEMPTS);
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
        return externalObjectDirectoryEntity;
    }

    @Override
    public List<TranscriptionStatusEntity> getFinishedTranscriptionStatuses() {
        List<TranscriptionStatusEntity> transcriptionStatuses = new ArrayList<>();
        transcriptionStatuses.add(getTranscriptionStatusById(CLOSED.getId()));
        transcriptionStatuses.add(getTranscriptionStatusById(COMPLETE.getId()));
        transcriptionStatuses.add(getTranscriptionStatusById(REJECTED.getId()));
        return transcriptionStatuses;
    }

    @Override
    public List<TranscriptionStatus> getTranscriptionStatuses() {
        List<TranscriptionStatusEntity> transcriptionStatusRepositoryAll = transcriptionStatusRepository.findAll();

        return transcriptionStatusRepositoryAll.stream().map(this::mapToTranscriptionStatus).toList();
    }

    @Override
    @Transactional
    public GetTranscriptionByIdResponse getTranscription(Long transcriptionId) {
        var userIsSuperAdmin = this.userIdentity.userHasGlobalAccess(Set.of(SUPER_ADMIN));
        TranscriptionEntity transcription = transcriptionRepository.findById(transcriptionId)
            .filter(TranscriptionEntity::getIsCurrent)
            // Only SUPER_ADMIN users are allowed to view transcription requests with hidden documents
            .filter(transcriptionEntity -> userIsSuperAdmin
                || transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionEntity.getId()).isEmpty()
            )
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));
        return transcriptionResponseMapper.mapToTranscriptionResponse(transcription);
    }

    @Override
    public List<UpdateTranscriptionsItem> updateTranscriptions(List<UpdateTranscriptionsItem> request) {

        final List<TranscriptionEntity> processed = processTranscriptionUpdates(request);

        List<UpdateTranscriptionsItem> unprocessedUpdates = new ArrayList<>(request);
        List<UpdateTranscriptionsItem> processedUpdates = getTranscriptionForIds(getTranscriptionIdsForEntities(processed), request);
        unprocessedUpdates.removeAll(processedUpdates);

        if (processedUpdates.isEmpty()) {
            log.error("All transcription updates failed");
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_UPDATE_TRANSCRIPTIONS);
        } else if (!unprocessedUpdates.isEmpty()) {
            // return a partial success
            for (UpdateTranscriptionsItem unprocessedUpdateItem : unprocessedUpdates) {
                log.error("Transcription update failed for transcription {}", unprocessedUpdateItem.getTranscriptionId());
            }

            throw PartialFailureException.getPartialPayloadJson(TranscriptionApiError.FAILED_TO_UPDATE_TRANSCRIPTIONS, unprocessedUpdates);
        }

        return processedUpdates;
    }

    @Override
    public List<TranscriptionDocumentEntity> getAllCaseTranscriptionDocuments(Integer caseId) {
        var transcriptions = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        var uniqueTranscriptionDocuments = new ArrayList<TranscriptionDocumentEntity>();
        for (var transcription : transcriptions) {
            var transcriptionDocuments = transcription.getTranscriptionDocumentEntities();
            for (var trDocument : transcriptionDocuments) {
                if (uniqueTranscriptionDocuments.stream().noneMatch(td -> td.getId().equals(trDocument.getId()))) {
                    uniqueTranscriptionDocuments.add(trDocument);
                }
            }
        }
        return uniqueTranscriptionDocuments;
    }

    @Override
    public List<Long> rollbackUserTranscriptions(UserAccountEntity entity) {
        List<TranscriptionEntity> transcriptionWorkflowEntities = transcriptionWorkflowRepository
            .findWorkflowForUserWithTranscriptionState(entity.getId(), WITH_TRANSCRIBER.getId());

        List<Long> transcriptionIds = new ArrayList<>();

        // add the workflows back
        for (TranscriptionEntity transcription : transcriptionWorkflowEntities) {
            saveTranscriptionWorkflow(entity, transcription,
                                      transcriptionStatusRepository.getReferenceById(APPROVED.getId()),
                                      OWNER_DISABLED_COMMENT_MESSAGE);
            transcriptionIds.add(transcription.getId());
        }

        return transcriptionIds;
    }

    @Override
    public List<AdminMarkedForDeletionResponseItem> adminGetTranscriptionDocumentsMarkedForDeletion() {
        if (!this.isManualDeletionEnabled()) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
        }
        List<TranscriptionDocumentEntity> transcriptionDocumentEntities = transcriptionDocumentRepository.getMarkedForDeletion();
        List<AdminMarkedForDeletionResponseItem> transcriptionResponsesLst = new ArrayList<>();
        for (TranscriptionDocumentEntity entity : transcriptionDocumentEntities) {
            transcriptionResponsesLst.add(transcriptionResponseMapper.mapTranscriptionDocumentMarkedForDeletion(entity));
        }
        transcriptionResponsesLst.sort((o1, o2) ->
                                           o2.getCase().getCaseNumber().compareTo(o1.getCase().getCaseNumber()));
        return transcriptionResponsesLst;
    }

    private List<TranscriptionEntity> processTranscriptionUpdates(List<UpdateTranscriptionsItem> request) {
        List<TranscriptionEntity> foundTranscriptionEntities = transcriptionRepository.findByIdIn(getTranscriptionIdsForRequest(request));
        final List<TranscriptionEntity> validated = new ArrayList<>();
        for (UpdateTranscriptionsItem requestItem : request) {
            Optional<TranscriptionEntity> matchingEntity = getTranscriptionEntityForId(requestItem.getTranscriptionId(), foundTranscriptionEntities);
            updateTranscriptionsValidator.forEach(validator -> {
                if (validator.validate(matchingEntity, requestItem)) {
                    validated.add(matchingEntity.get());
                }
            });
        }

        if (!validated.isEmpty()) {
            validated.forEach(entity -> UpdateTranscriptionEntityHelper.updateTranscriptionEntity(
                entity,
                getTranscriptionsItemForId(entity.getId(), request).get()
            ));

            transcriptionRepository.saveAll(validated);
        }
        return validated;
    }

    private List<Long> getTranscriptionIdsForRequest(List<UpdateTranscriptionsItem> updateTranscriptionsItems) {
        return updateTranscriptionsItems.stream().map(UpdateTranscriptionsItem::getTranscriptionId).toList();
    }

    private List<Long> getTranscriptionIdsForEntities(List<TranscriptionEntity> transcriptionEntities) {
        return transcriptionEntities.stream().map(TranscriptionEntity::getId).toList();
    }

    private List<UpdateTranscriptionsItem> getTranscriptionForIds(List<Long> transcriptionIds, List<UpdateTranscriptionsItem> updateTranscriptionsItems) {
        return updateTranscriptionsItems.stream().filter(e -> transcriptionIds.contains(e.getTranscriptionId())).toList();
    }

    private Optional<UpdateTranscriptionsItem> getTranscriptionsItemForId(Long transcriptionId, List<UpdateTranscriptionsItem> updateTranscriptions) {
        return updateTranscriptions.stream().filter(e -> e.getTranscriptionId().equals(transcriptionId)).findAny();
    }

    private Optional<TranscriptionEntity> getTranscriptionEntityForId(Long transcriptionId,
                                                                      List<TranscriptionEntity> updateTranscriptions) {
        return updateTranscriptions.stream().filter(e ->
                                                        e.getId().equals(transcriptionId)).findAny();
    }

    private TranscriptionStatus mapToTranscriptionStatus(TranscriptionStatusEntity transcriptionStatusEntity) {
        TranscriptionStatus transcriptionStatus = new TranscriptionStatus();
        transcriptionStatus.setId(transcriptionStatusEntity.getId());
        transcriptionStatus.setType(transcriptionStatusEntity.getStatusType());
        transcriptionStatus.setDisplayName(transcriptionStatusEntity.getDisplayName());
        return transcriptionStatus;
    }

}