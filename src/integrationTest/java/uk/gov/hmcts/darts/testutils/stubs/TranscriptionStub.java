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
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
@SuppressWarnings("PMD.GodClass")
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
    private final UserAccountStub userAccountStub;
    private final HearingStub hearingStub;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;

    public TranscriptionEntity createMinimalTranscription() {
        return createTranscription(hearingStub.createMinimalHearing());
    }

    public TranscriptionEntity createTranscription(HearingEntity hearing) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);
        UserAccountEntity authorisedIntegrationTestUser = userAccountStub.createAuthorisedIntegrationTestUser(hearing.getCourtCase()
                                                                                                                  .getCourthouse());
        return createAndSaveTranscriptionEntity(
            hearing,
            transcriptionType,
            transcriptionStatus,
            Optional.of(transcriptionUrgencyEntity),
            authorisedIntegrationTestUser
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
                                                   TranscriptionWorkflowEntity workflowEntity,
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

    private TranscriptionStatusEntity mapToTranscriptionStatusEntity(TranscriptionStatusEnum statusEnum) {
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
        TranscriptionEntity transcription = new TranscriptionEntity();

        if (hearing != null) {
            transcription.setCourtroom(hearing.getCourtroom());
            transcription.addHearing(hearing);
        }

        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);

        if (transcriptionUrgency.isPresent()) {
            transcription.setTranscriptionUrgency(transcriptionUrgency.get());
        } else {
            transcription.setTranscriptionUrgency(null);
        }

        transcription.setCreatedDateTime(now());
        transcription.setRequestor(testUser.getId().toString());
        transcription.setCreatedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(true);
        transcription.setHideRequestFromRequestor(false);

        if (hearing != null) {
            hearing.getTranscriptions().add(transcription);
        }

        return transcriptionRepository.saveAndFlush(transcription);
    }


    public TranscriptionEntity createAndSaveTranscriptionEntity(List<HearingEntity> hearing, List<CourtCaseEntity> courtCase,
                                                                CourtroomEntity courtroomEntity,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                TranscriptionUrgencyEntity transcriptionUrgency,
                                                                UserAccountEntity testUser,
                                                                TranscriptionWorkflowEntity workflowEntity,
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
            workflowEntity.setTranscription(transcription);

            transcription.getTranscriptionWorkflowEntities().add(workflowEntity);
        }

        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setCreatedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(isManualTranscription);
        transcription.setHideRequestFromRequestor(false);
        return transcriptionRepository.saveAndFlush(transcription);
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
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(true);
        transcription.setHideRequestFromRequestor(false);
        return transcriptionRepository.saveAndFlush(transcription);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
    }

    @Transactional
    public TranscriptionEntity createAndSaveAwaitingAuthorisationTranscription(UserAccountEntity userAccountEntity,
                                                                               CourtCaseEntity courtCaseEntity,
                                                                               HearingEntity hearingEntity,
                                                                               OffsetDateTime workflowTimestamp,
                                                                               boolean associatedUrgency) {
        var transcriptionEntity = this.createTranscriptionWithStatus(
            userAccountEntity,
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            null,
            associatedUrgency
        );
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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

        var transcriptionWorkflow = createTranscriptionWorkflowEntity(transcription, transcription.getCreatedBy(), workflowTimestamp, transcriptionStatus);

        return transcriptionWorkflowRepository.save(transcriptionWorkflow);
    }

    public TranscriptionEntity updateTranscriptionWithDocument(TranscriptionEntity transcriptionEntity,
                                                               ObjectRecordStatusEnum status,
                                                               ExternalLocationTypeEnum location,
                                                               UUID eodExternalLocation) {
        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 10;
        final ObjectRecordStatusEntity objectRecordStatusEntity = getStatusEntity(status);
        final ExternalLocationTypeEntity externalLocationTypeEntity = getLocationEntity(location);

        return updateTranscriptionWithDocument(transcriptionEntity,
                                               fileName,
                                               fileType,
                                               fileSize,
                                               transcriptionEntity.getCreatedBy(),
                                               objectRecordStatusEntity,
                                               externalLocationTypeEntity,
                                               eodExternalLocation,
                                               getChecksum());
    }

    @Transactional
    public TranscriptionEntity updateTranscriptionWithDocument(TranscriptionEntity transcriptionEntity,
                                                               String fileName,
                                                               String fileType,
                                                               int fileSize,
                                                               UserAccountEntity testUser,
                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                               ExternalLocationTypeEntity externalLocationTypeEntity,
                                                               UUID externalLocation,
                                                               String checksum) {

        TranscriptionDocumentEntity transcriptionDocumentEntity = createTranscriptionDocumentEntity(transcriptionEntity, fileName,
                                                                                                    fileType, fileSize, testUser,
                                                                                                    checksum);
        transcriptionDocumentRepository.save(transcriptionDocumentEntity);
        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);
        transcriptionRepository.save(transcriptionEntity);

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
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);

        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryEntity);
        return transcriptionRepository.save(transcriptionEntity);
    }

    private String getChecksum() {
        return TestUtils.encodeToString(md5(TRANSCRIPTION_TEST_DATA_BINARY_DATA));
    }

    public static TranscriptionDocumentEntity createTranscriptionDocumentEntity(TranscriptionEntity transcriptionEntity, String fileName, String fileType,
                                                                                int fileSize, UserAccountEntity testUser, String checksum) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName(fileName);
        transcriptionDocumentEntity.setFileType(fileType);
        transcriptionDocumentEntity.setFileSize(fileSize);
        transcriptionDocumentEntity.setUploadedBy(testUser);
        transcriptionDocumentEntity.setUploadedDateTime(now(UTC));
        transcriptionDocumentEntity.setChecksum(checksum);
        transcriptionDocumentEntity.setLastModifiedBy(testUser);
        return transcriptionDocumentEntity;
    }

    public static TranscriptionDocumentEntity createTranscriptionDocumentEntity(TranscriptionEntity transcriptionEntity, String fileName, String fileType,
                                                                                int fileSize, UserAccountEntity testUser, String checksum,
                                                                                OffsetDateTime uploadedDateTime) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum);
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
        transcriptionEntity.setLastModifiedBy(userAccountEntity);
        transcriptionEntity.setIsManualTranscription(true);
        transcriptionEntity.setHideRequestFromRequestor(false);
        transcriptionRepository.save(transcriptionEntity);

        final var requestedTranscriptionWorkflowEntity = createTranscriptionWorkflowEntity(
            transcriptionEntity,
            userAccountEntity,
            workflowTimestamp,
            getTranscriptionStatusByEnum(REQUESTED)
        );
        transcriptionWorkflowRepository.saveAndFlush(requestedTranscriptionWorkflowEntity);

        if (nonNull(comment)) {
            final var transcriptionComment = createTranscriptionWorkflowComment(requestedTranscriptionWorkflowEntity, comment, userAccountEntity);
            transcriptionCommentRepository.save(transcriptionComment);

            requestedTranscriptionWorkflowEntity.getTranscriptionComments().add(transcriptionComment);
        }

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = createTranscriptionWorkflowEntity(
            transcriptionEntity,
            userAccountEntity,
            workflowTimestamp,
            status
        );
        transcriptionWorkflowRepository.saveAndFlush(transcriptionWorkflowEntity);

        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(requestedTranscriptionWorkflowEntity, transcriptionWorkflowEntity));
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
    }

    public TranscriptionCommentEntity createTranscriptionWorkflowComment(TranscriptionWorkflowEntity workflowEntity, String comment,
                                                                         UserAccountEntity userAccountEntity) {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setTranscription(workflowEntity.getTranscription());
        transcriptionCommentEntity.setTranscriptionWorkflow(workflowEntity);
        transcriptionCommentEntity.setComment(comment);
        transcriptionCommentEntity.setCommentTimestamp(workflowEntity.getWorkflowTimestamp());
        transcriptionCommentEntity.setAuthorUserId(userAccountEntity.getId());
        transcriptionCommentEntity.setLastModifiedBy(userAccountEntity);
        transcriptionCommentEntity.setCreatedBy(userAccountEntity);
        return transcriptionCommentEntity;
    }

    public TranscriptionCommentEntity createAndSaveTranscriptionWorkflowComment(TranscriptionWorkflowEntity workflowEntity,
                                                                                String comment, UserAccountEntity userAccount) {
        var transcriptionComment = createTranscriptionWorkflowComment(workflowEntity, comment, userAccount);
        return transcriptionCommentRepository.save(transcriptionComment);
    }

    public TranscriptionCommentEntity createAndSaveTranscriptionCommentNotAssociatedToWorkflow(TranscriptionEntity transcription,
                                                                                               OffsetDateTime commentTimestamp,
                                                                                               String comment) {
        TranscriptionCommentEntity transcriptionComment = new TranscriptionCommentEntity();
        transcriptionComment.setTranscription(transcription);
        transcriptionComment.setComment(comment);
        transcriptionComment.setCommentTimestamp(commentTimestamp);
        transcriptionComment.setAuthorUserId(transcription.getCreatedBy().getId());
        transcriptionComment.setLastModifiedBy(transcription.getCreatedBy());
        transcriptionComment.setCreatedBy(transcription.getCreatedBy());
        return transcriptionCommentRepository.save(transcriptionComment);
    }

    private ExternalLocationTypeEntity getLocationEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    private ObjectRecordStatusEntity getStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }
}