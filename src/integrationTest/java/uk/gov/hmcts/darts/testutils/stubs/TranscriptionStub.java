package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
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
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.nonNull;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Component
@RequiredArgsConstructor
@Deprecated
@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class TranscriptionStub {

    public static final byte[] TRANSCRIPTION_TEST_DATA_BINARY_DATA = "test binary data".getBytes();

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionCommentRepository transcriptionCommentRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final UserAccountStubComposable userAccountStub;
    private final HearingStub hearingStub;
    private final TranscriptionStubComposable transcriptionStubComposable;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;
    private final UserAccountRepository userAccountRepository;

    public TranscriptionEntity createMinimalTranscription() {
        return createTranscription(hearingStub.createMinimalHearing());
    }

    public TranscriptionEntity createTranscription(HearingEntity hearing) {
        return transcriptionStubComposable.createTranscription(userAccountStub, hearing);
    }

    public TranscriptionEntity createTranscription(CourtroomEntity courtroomEntity) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);

        UserAccountEntity authorisedIntegrationTestUser = userAccountStub
            .createAuthorisedIntegrationTestUser(courtroomEntity != null ? courtroomEntity.getCourthouse() : null);
        return transcriptionStubComposable.createAndSaveTranscriptionEntity(
            null,
            transcriptionType,
            transcriptionStatus,
            Optional.of(transcriptionUrgencyEntity),
            authorisedIntegrationTestUser,
            courtroomEntity
        );
    }

    public TranscriptionEntity createTranscription(HearingEntity hearing, UserAccountEntity userAccountEntity) {
        return createTranscription(hearing, userAccountEntity, APPROVED);
    }

    public TranscriptionEntity createTranscription(HearingEntity hearing, UserAccountEntity userAccountEntity, TranscriptionStatusEnum statusEnum) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(statusEnum);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);

        return createAndSaveTranscriptionEntity(
            hearing,
            transcriptionType,
            transcriptionStatus,
            Optional.of(transcriptionUrgencyEntity),
            userAccountEntity
        );
    }

    public TranscriptionEntity createTranscription(HearingEntity hearing, boolean associateUrgency) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);
        UserAccountEntity authorisedIntegrationTestUser = userAccountStub.createAuthorisedIntegrationTestUser(hearing.getCourtCase()
                                                                                                                  .getCourthouse());
        return createAndSaveTranscriptionEntity(
            hearing,
            transcriptionType,
            transcriptionStatus,
            associateUrgency ? Optional.of(transcriptionUrgencyEntity) : Optional.empty(),
            authorisedIntegrationTestUser
        );

    }

    public TranscriptionEntity createTranscription(CourtCaseEntity courtCase) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);
        UserAccountEntity authorisedIntegrationTestUser = userAccountStub.createAuthorisedIntegrationTestUser(courtCase
                                                                                                                  .getCourthouse());
        return createAndSaveTranscriptionEntity(null,
                                                List.of(courtCase),
                                                null,
                                                transcriptionType,
                                                transcriptionStatus,
                                                transcriptionUrgencyEntity,
                                                authorisedIntegrationTestUser,
                                                null, true
        );
    }

    public TranscriptionEntity createTranscription(List<HearingEntity> hearingEntityList,
                                                   List<CourtCaseEntity> courtCaseLst,
                                                   CourtroomEntity courtroomEntity,
                                                   UserAccountEntity userAccountEntity,
                                                   List<TranscriptionWorkflowEntity> workflowEntity,
                                                   Boolean isManualTranscription) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);

        return createAndSaveTranscriptionEntity(hearingEntityList,
                                                courtCaseLst,
                                                courtroomEntity,
                                                transcriptionType,
                                                transcriptionStatus,
                                                transcriptionUrgencyEntity,
                                                userAccountEntity,
                                                workflowEntity,
                                                isManualTranscription
        );
    }

    private TranscriptionUrgencyEntity mapToTranscriptionUrgencyEntity(TranscriptionUrgencyEnum urgencyEnum) {
        return transcriptionUrgencyRepository.findById(urgencyEnum.getId()).get();
    }

    private TranscriptionTypeEntity mapToTranscriptionTypeEntity(TranscriptionTypeEnum typeEnum) {
        TranscriptionTypeEntity transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(typeEnum.getId());
        transcriptionType.setDescription(typeEnum.name());
        return transcriptionType;
    }

    public TranscriptionStatusEntity mapToTranscriptionStatusEntity(TranscriptionStatusEnum statusEnum) {
        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(statusEnum.getId());
        transcriptionStatus.setStatusType(statusEnum.name());
        transcriptionStatus.setDisplayName(statusEnum.name());
        return transcriptionStatus;
    }

    public TranscriptionEntity createAndSaveTranscriptionEntity(HearingEntity hearing,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                Optional<TranscriptionUrgencyEntity> transcriptionUrgency,
                                                                UserAccountEntity testUser) {
        return createAndSaveTranscriptionEntity(hearing, transcriptionType, transcriptionStatus, transcriptionUrgency, testUser, null);
    }

    public TranscriptionEntity createAndSaveTranscriptionEntity(HearingEntity hearing,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                Optional<TranscriptionUrgencyEntity> transcriptionUrgency,
                                                                UserAccountEntity testUser,
                                                                CourtroomEntity courtroomEntity) {
        TranscriptionEntity transcription = new TranscriptionEntity();

        if (hearing != null) {
            transcription.setCourtroom(hearing.getCourtroom());
            transcription.addHearing(hearing);
        }

        if (courtroomEntity != null) {
            transcription.setCourtroom(courtroomEntity);
        }

        transcription.setLegacyObjectId("legacyObjectId");
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);

        if (transcriptionUrgency.isPresent()) {
            transcription.setTranscriptionUrgency(transcriptionUrgency.get());
        } else {
            transcription.setTranscriptionUrgency(null);
        }

        transcription.setCreatedDateTime(now());
        transcription.setRequestedBy(testUser);
        transcription.setCreatedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(true);
        transcription.setHideRequestFromRequestor(false);
        transcription.setIsCurrent(true);

        if (hearing != null) {
            hearing.getTranscriptions().add(transcription);
        }

        return dartsDatabaseSaveStub.save(transcription);
    }


    public TranscriptionEntity createAndSaveTranscriptionEntity(List<HearingEntity> hearing, List<CourtCaseEntity> courtCase,
                                                                CourtroomEntity courtroomEntity,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                TranscriptionUrgencyEntity transcriptionUrgency,
                                                                UserAccountEntity testUser,
                                                                List<TranscriptionWorkflowEntity> workflowEntity,
                                                                boolean isManualTranscription) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        for (CourtCaseEntity caseEntity : courtCase) {
            transcription.addCase(caseEntity);
        }

        if (hearing != null) {
            for (HearingEntity hearingEntity : hearing) {
                transcription.addHearing(hearingEntity);
            }
        }

        if (courtroomEntity != null) {
            transcription.setCourtroom(courtroomEntity);
        }

        if (workflowEntity != null) {

            for (TranscriptionWorkflowEntity wfe : workflowEntity) {
                wfe.setTranscription(transcription);

                transcription.getTranscriptionWorkflowEntities().add(wfe);
            }
        }

        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setCreatedBy(testUser);
        transcription.setRequestedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(isManualTranscription);
        transcription.setHideRequestFromRequestor(false);
        transcription.setIsCurrent(true);
        return dartsDatabaseSaveStub.save(transcription);
    }

    public TranscriptionEntity createAndSaveTranscriptionEntity(CourtCaseEntity courtCase,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                TranscriptionUrgencyEntity transcriptionUrgency,
                                                                UserAccountEntity testUser) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.addCase(courtCase);
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setCreatedBy(testUser);
        transcription.setRequestedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(true);
        transcription.setHideRequestFromRequestor(false);
        transcription.setIsCurrent(true);
        return dartsDatabaseSaveStub.save(transcription);
    }

    @Transactional
    public TranscriptionEntity createAndSaveAwaitingAuthorisationTranscription(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               OffsetDateTime workflowTimestamp) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            null
        );
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveAwaitingAuthorisationTranscription(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               String comment,
                                                                               OffsetDateTime workflowTimestamp) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            comment
        );
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveAwaitingAuthorisationTranscription(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               OffsetDateTime workflowTimestamp,
                                                                               boolean associatedUrgency) {
        return createAndSaveAwaitingAuthorisationTranscription(userAccountEntity, courtCaseEntity,
                                                               hearingEntity, workflowTimestamp, associatedUrgency, true);
    }

    @Transactional
    public TranscriptionEntity createAndSaveAwaitingAuthorisationTranscription(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               OffsetDateTime workflowTimestamp,
                                                                               boolean associatedUrgency, boolean isCurrent) {
        return this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            null,
            associatedUrgency,
            isCurrent
        );
    }

    @Transactional
    public TranscriptionEntity createAndSaveApprovedTranscription(UserAccountEntity userAccountEntity,
                                                                  CourtCaseEntity courtCaseEntity,
                                                                  HearingEntity hearingEntity,
                                                                  OffsetDateTime workflowTimestamp,
                                                                  Boolean hideRequestFromRequester) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(APPROVED),
            null
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveWithTranscriberTranscription(UserAccountEntity userAccountEntity,
                                                                         CourtCaseEntity courtCaseEntity,
                                                                         HearingEntity hearingEntity,
                                                                         OffsetDateTime workflowTimestamp,
                                                                         Boolean hideRequestFromRequester) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(WITH_TRANSCRIBER),
            null
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveCompletedTranscription(UserAccountEntity userAccountEntity,
                                                                   CourtCaseEntity courtCaseEntity,
                                                                   HearingEntity hearingEntity,
                                                                   OffsetDateTime workflowTimestamp,
                                                                   Boolean hideRequestFromRequester) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(COMPLETE),
            null
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveCompletedTranscription(UserAccountEntity userAccountEntity,
                                                                   CourtCaseEntity courtCaseEntity,
                                                                   HearingEntity hearingEntity,
                                                                   OffsetDateTime startDate,
                                                                   OffsetDateTime endDate,
                                                                   OffsetDateTime workflowTimestamp,
                                                                   Boolean hideRequestFromRequester) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(COMPLETE),
            null
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        transcriptionEntity.setStartTime(startDate);
        transcriptionEntity.setEndTime(endDate);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveCompletedTranscription(UserAccountEntity userAccountEntity,
                                                                   CourtCaseEntity courtCaseEntity,
                                                                   HearingEntity hearingEntity,
                                                                   OffsetDateTime startDate,
                                                                   OffsetDateTime endDate,
                                                                   TranscriptionTypeEntity transcriptionType,
                                                                   OffsetDateTime workflowTimestamp,
                                                                   Boolean hideRequestFromRequester) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(COMPLETE),
            null
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        transcriptionEntity.setStartTime(startDate);
        transcriptionEntity.setEndTime(endDate);
        transcriptionEntity.setTranscriptionType(transcriptionType);
        transcriptionEntity.setIsCurrent(true);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveCompletedTranscriptionWithDocument(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               OffsetDateTime workflowTimestamp,
                                                                               Boolean hideDocument) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(COMPLETE),
            null
        );

        transcriptionEntity = updateTranscriptionWithDocument(transcriptionEntity, userAccountEntity, hideDocument);

        return transcriptionEntity;
    }

    public TranscriptionWorkflowEntity createTranscriptionWorkflowEntity(TranscriptionEntity transcription,
                                                                         Integer userId,
                                                                         OffsetDateTime workflowTimestamp,
                                                                         TranscriptionStatusEntity transcriptionStatus) {
        UserAccountEntity user = userAccountRepository.findById(userId).orElseThrow();
        return createTranscriptionWorkflowEntity(transcription, user, workflowTimestamp, transcriptionStatus);
    }

    public TranscriptionWorkflowEntity createTranscriptionWorkflowEntity(TranscriptionEntity transcription,
                                                                         UserAccountEntity user,
                                                                         OffsetDateTime workflowTimestamp,
                                                                         TranscriptionStatusEntity transcriptionStatus) {
        TranscriptionWorkflowEntity transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(transcription);
        transcriptionWorkflow.setTranscriptionStatus(transcriptionStatus);
        transcriptionWorkflow.setWorkflowActor(user);
        transcriptionWorkflow.setWorkflowTimestamp(workflowTimestamp);
        return transcriptionWorkflow;
    }

    public TranscriptionWorkflowEntity createAndSaveTranscriptionWorkflow(TranscriptionEntity transcription,
                                                                          OffsetDateTime workflowTimestamp,
                                                                          TranscriptionStatusEntity transcriptionStatus) {

        var transcriptionWorkflow = createTranscriptionWorkflowEntity(transcription, transcription.getCreatedById(), workflowTimestamp, transcriptionStatus);

        return dartsDatabaseSaveStub.save(transcriptionWorkflow);
    }

    public TranscriptionEntity updateTranscriptionWithDocument(TranscriptionEntity transcriptionEntity,
                                                               UserAccountEntity userAccountEntity, Boolean hideDocument) {

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName("aFilename");
        transcriptionDocumentEntity.setFileType("aFileType");
        transcriptionDocumentEntity.setFileSize(100);
        transcriptionDocumentEntity.setChecksum("");
        transcriptionDocumentEntity.setHidden(hideDocument);
        transcriptionDocumentEntity.setUploadedBy(userAccountEntity);
        transcriptionDocumentEntity.setLastModifiedBy(userAccountEntity);
        dartsDatabaseSaveStub.save(transcriptionDocumentEntity);

        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);
        dartsDatabaseSaveStub.save(transcriptionEntity);

        return transcriptionEntity;
    }

    public TranscriptionEntity updateTranscriptionWithDocument(TranscriptionEntity transcriptionEntity,
                                                               ObjectRecordStatusEnum status,
                                                               ExternalLocationTypeEnum location,
                                                               String eodExternalLocation) {
        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 10;
        final ObjectRecordStatusEntity objectRecordStatusEntity = getStatusEntity(status);
        final ExternalLocationTypeEntity externalLocationTypeEntity = getLocationEntity(location);
        final String confidenceReason = "reason";
        final RetentionConfidenceScoreEnum confidenceScore = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

        UserAccountEntity userAccount = userAccountRepository.findById(transcriptionEntity.getCreatedById()).orElseThrow();
        return updateTranscriptionWithDocument(transcriptionEntity,
                                               fileName,
                                               fileType,
                                               fileSize,
                                               userAccount,
                                               objectRecordStatusEntity,
                                               externalLocationTypeEntity,
                                               eodExternalLocation,
                                               getChecksum(), confidenceScore,
                                               confidenceReason);
    }

    @Transactional
    @SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.UseObjectForClearerAPI"})
    public TranscriptionEntity updateTranscriptionWithDocument(TranscriptionEntity transcriptionEntity,
                                                               String fileName,
                                                               String fileType,
                                                               int fileSize,
                                                               UserAccountEntity testUser,
                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                               ExternalLocationTypeEntity externalLocationTypeEntity,
                                                               String externalLocation,
                                                               String checksum,
                                                               RetentionConfidenceScoreEnum confScore,
                                                               String confReason
    ) {

        TranscriptionDocumentEntity transcriptionDocumentEntity = createTranscriptionDocumentEntity(transcriptionEntity, fileName,
                                                                                                    fileType, fileSize, testUser,
                                                                                                    checksum, confScore, confReason);
        dartsDatabaseSaveStub.save(transcriptionDocumentEntity);
        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);
        dartsDatabaseSaveStub.save(transcriptionEntity);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(checksum);
        externalObjectDirectoryEntity.setTransferAttempts(null);
        externalObjectDirectoryEntity.setVerificationAttempts(1);
        externalObjectDirectoryEntity.setCreatedBy(testUser);
        externalObjectDirectoryEntity.setLastModifiedBy(testUser);
        externalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        externalObjectDirectoryEntity = dartsDatabaseSaveStub.save(externalObjectDirectoryEntity);

        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryEntity);
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    private String getChecksum() {
        return TestUtils.encodeToString(md5(TRANSCRIPTION_TEST_DATA_BINARY_DATA));
    }

    public static TranscriptionDocumentEntity createTranscriptionDocumentEntity(TranscriptionEntity transcriptionEntity, String fileName, String fileType,
                                                                                int fileSize, UserAccountEntity testUser, String checksum) {
        return createTranscriptionDocumentEntity(transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, null, null);
    }


    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static TranscriptionDocumentEntity createTranscriptionDocumentEntity(TranscriptionEntity transcriptionEntity, String fileName, String fileType,
                                                                                int fileSize,
                                                                                UserAccountEntity testUser,
                                                                                String checksum, RetentionConfidenceScoreEnum confScore, String confReason) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName(fileName);
        transcriptionDocumentEntity.setFileType(fileType);
        transcriptionDocumentEntity.setFileSize(fileSize);
        transcriptionDocumentEntity.setUploadedBy(testUser);
        transcriptionDocumentEntity.setUploadedDateTime(now(UTC));
        transcriptionDocumentEntity.setChecksum(checksum);
        transcriptionDocumentEntity.setLastModifiedBy(testUser);
        transcriptionDocumentEntity.setRetConfScore(confScore);
        transcriptionDocumentEntity.setRetConfReason(confReason);

        return transcriptionDocumentEntity;
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static TranscriptionDocumentEntity createTranscriptionDocumentEntity(TranscriptionEntity transcriptionEntity, String fileName, String fileType,
                                                                                int fileSize, UserAccountEntity testUser, String checksum,
                                                                                OffsetDateTime uploadedDateTime) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED,
            "confidence reason");
        transcriptionDocumentEntity.setUploadedDateTime(uploadedDateTime);
        return transcriptionDocumentEntity;
    }


    public TranscriptionStatusEntity getTranscriptionStatusByEnum(TranscriptionStatusEnum transcriptionStatusEnum) {
        return transcriptionStatusRepository.getReferenceById(transcriptionStatusEnum.getId());
    }

    public TranscriptionTypeEntity getTranscriptionTypeByEnum(TranscriptionTypeEnum transcriptionTypeEnum) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId());
    }

    public TranscriptionUrgencyEntity getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum transcriptionUrgencyEnum) {
        return transcriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId());
    }

    private TranscriptionEntity createTranscriptionWithStatus(UserAccountEntity userAccountEntity,
                                                              CourtCaseEntity courtCaseEntity,
                                                              HearingEntity hearingEntity,
                                                              OffsetDateTime workflowTimestamp,
                                                              TranscriptionStatusEntity status,
                                                              String comment) {
        return createTranscriptionWithStatus(userAccountEntity, courtCaseEntity, hearingEntity, workflowTimestamp, status, comment, true);
    }

    private TranscriptionEntity createTranscriptionWithStatus(UserAccountEntity userAccountEntity,
                                                              CourtCaseEntity courtCaseEntity,
                                                              HearingEntity hearingEntity,
                                                              OffsetDateTime workflowTimestamp,
                                                              TranscriptionStatusEntity status,
                                                              String comment, boolean associateUrgency) {

        return createTranscriptionWithStatus(userAccountEntity, courtCaseEntity, hearingEntity, workflowTimestamp,
                                             status, comment, associateUrgency, true);
    }

    private TranscriptionEntity createTranscriptionWithStatus(UserAccountEntity userAccountEntity,
                                                              CourtCaseEntity courtCaseEntity,
                                                              HearingEntity hearingEntity,
                                                              OffsetDateTime workflowTimestamp,
                                                              TranscriptionStatusEntity status,
                                                              String comment, boolean associateUrgency, boolean isCurrent) {
        final var transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.addCase(courtCaseEntity);
        transcriptionEntity.addHearing(hearingEntity);
        transcriptionEntity.setTranscriptionType(getTranscriptionTypeByEnum(SPECIFIED_TIMES));

        if (associateUrgency) {
            transcriptionEntity.setTranscriptionUrgency(getTranscriptionUrgencyByEnum(STANDARD));
        }
        transcriptionEntity.setTranscriptionStatus(status);
        OffsetDateTime now = now(UTC);
        OffsetDateTime yesterday = now(UTC).minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        transcriptionEntity.setStartTime(yesterday);
        transcriptionEntity.setEndTime(yesterday.plusHours(now.getHour()).plusMinutes(now.getMinute())
                                           .plusSeconds(now.getSecond()).plusNanos(now.getNano()));
        transcriptionEntity.setCreatedBy(userAccountEntity);
        transcriptionEntity.setRequestedBy(userAccountEntity);
        transcriptionEntity.setLastModifiedBy(userAccountEntity);
        transcriptionEntity.setIsManualTranscription(true);
        transcriptionEntity.setHideRequestFromRequestor(false);
        transcriptionEntity.setIsCurrent(isCurrent);
        dartsDatabaseSaveStub.save(transcriptionEntity);

        final var requestedTranscriptionWorkflowEntity = createTranscriptionWorkflowEntity(
            transcriptionEntity,
            userAccountEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(REQUESTED)
        );
        dartsDatabaseSaveStub.save(requestedTranscriptionWorkflowEntity);

        if (nonNull(comment)) {
            final var transcriptionComment = createTranscriptionWorkflowComment(requestedTranscriptionWorkflowEntity, comment, userAccountEntity.getId());
            dartsDatabaseSaveStub.save(transcriptionComment);

            requestedTranscriptionWorkflowEntity.getTranscriptionComments().add(transcriptionComment);
        }

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = createTranscriptionWorkflowEntity(
            transcriptionEntity,
            userAccountEntity,
            workflowTimestamp,
            status
        );
        dartsDatabaseSaveStub.save(transcriptionWorkflowEntity);

        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(requestedTranscriptionWorkflowEntity, transcriptionWorkflowEntity));
        return dartsDatabaseSaveStub.save(transcriptionEntity);
    }

    public TranscriptionCommentEntity createTranscriptionWorkflowComment(TranscriptionWorkflowEntity workflowEntity, String comment,
                                                                         Integer userAccountEntityId) {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setTranscription(workflowEntity.getTranscription());
        transcriptionCommentEntity.setTranscriptionWorkflow(workflowEntity);
        transcriptionCommentEntity.setComment(comment);
        transcriptionCommentEntity.setCommentTimestamp(workflowEntity.getWorkflowTimestamp());
        transcriptionCommentEntity.setAuthorUserId(userAccountEntityId);
        transcriptionCommentEntity.setLastModifiedById(userAccountEntityId);
        transcriptionCommentEntity.setCreatedById(userAccountEntityId);
        return transcriptionCommentEntity;
    }

    public TranscriptionCommentEntity createAndSaveTranscriptionWorkflowComment(TranscriptionWorkflowEntity workflowEntity,
                                                                                String comment, Integer userAccountEntityId) {
        var transcriptionComment = createTranscriptionWorkflowComment(workflowEntity, comment, userAccountEntityId);
        return dartsDatabaseSaveStub.save(transcriptionComment);
    }

    public TranscriptionCommentEntity createAndSaveTranscriptionCommentNotAssociatedToWorkflow(TranscriptionEntity transcription,
                                                                                               OffsetDateTime commentTimestamp,
                                                                                               String comment) {
        TranscriptionCommentEntity transcriptionComment = new TranscriptionCommentEntity();
        transcriptionComment.setTranscription(transcription);
        transcriptionComment.setComment(comment);
        transcriptionComment.setCommentTimestamp(commentTimestamp);
        transcriptionComment.setAuthorUserId(transcription.getCreatedById());
        transcriptionComment.setLastModifiedById(transcription.getCreatedById());
        transcriptionComment.setCreatedById(transcription.getCreatedById());
        return dartsDatabaseSaveStub.save(transcriptionComment);
    }

    private ExternalLocationTypeEntity getLocationEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    private ObjectRecordStatusEntity getStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }

    public TranscriptionLinkedCaseEntity transcriptionLinkedCaseEntity(TranscriptionEntity transcriptionEntity, CourtCaseEntity courtCaseEntity,
                                                                       String courthouseName, String caseNumber) {
        TranscriptionLinkedCaseEntity transcriptionLinkedCaseEntity = new TranscriptionLinkedCaseEntity();
        transcriptionLinkedCaseEntity.setTranscription(transcriptionEntity);
        transcriptionLinkedCaseEntity.setCourtCase(courtCaseEntity);
        transcriptionLinkedCaseEntity.setCourthouseName(courthouseName);
        transcriptionLinkedCaseEntity.setCaseNumber(caseNumber);
        return dartsDatabaseSaveStub.save(transcriptionLinkedCaseEntity);
    }
}