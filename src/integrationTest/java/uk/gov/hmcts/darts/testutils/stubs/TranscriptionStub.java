package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
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
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentSubStringQueryEnum;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.testutils.TestUtils;
import uk.gov.hmcts.darts.testutils.data.UserAccountTestData;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    private final UserAccountRepository userAccountRepository;
    private final CourtroomStub courtroomStub;
    private final CourthouseStub courthouseStub;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourtCaseStub courtCaseStub;

    private static final String CASE_NUMBER_PREFIX = "CaseNumber";

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
        return createAndSaveTranscriptionEntity(
            courtCase,
            transcriptionType,
            transcriptionStatus,
            transcriptionUrgencyEntity,
            authorisedIntegrationTestUser
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
                                                               ExternalLocationTypeEnum location) {
        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 10;
        final ObjectRecordStatusEntity objectRecordStatusEntity = getStatusEntity(status);
        final ExternalLocationTypeEntity externalLocationTypeEntity = getLocationEntity(location);
        final UUID externalLocation = UUID.randomUUID();

        return updateTranscriptionWithDocument(transcriptionEntity,
                                               fileName,
                                               fileType,
                                               fileSize,
                                               transcriptionEntity.getCreatedBy(),
                                               objectRecordStatusEntity,
                                               externalLocationTypeEntity,
                                               externalLocation,
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

/**
 * generates test data. The following will be used for generation:-
 * Unique owner and requested by users for each transformed media record
 * Unique court house with unique name for each transformed media record
 * Unique case number with unique case number for each transformed media record
 * Unique hearing date starting with today with an incrementing day for each transformed media record
 * Unique requested date with an incrementing hour for each transformed media record
 * Unique file name with unique name for each transformed media record
 * @param count The number of transformed media objects that are to be generated
 * @return The list of generated media entities in chronological order
 */
public List<TranscriptionCommentEntity> generateTransformedMediaEntities(int count, int hearingCount, int caseCount, boolean hidden, boolean isManualTranscription) {

    UserAccountEntity accountEntity = UserAccountTestData.minimalUserAccount();
    userAccountRepository.save(accountEntity);

    CourtroomEntity courtroomEntity = courtroomStub.createCourtroomUnlessExists("Newcastle", "room_a");
    CourthouseEntity courthouse = courthouseStub.createCourthouseUnlessExists("");
    HearingEntity hearingEntity =  retrieveCoreObjectService.retrieveOrCreateHearing(
        courtroomEntity.getCourthouse().getCourthouseName(),
        courtroomEntity.getName(),
        "c1",
        LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        userAccountRepository.getReferenceById(0)
    );

    CourtCaseEntity caseEntity = courtCaseStub.createAndSaveMinimalCourtCase();
    caseEntity.setCaseNumber("");

    List<TranscriptionDocumentEntity> retTransformerMediaLst = new ArrayList<>();
    OffsetDateTime hoursBefore = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime hoursAfter = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime requestedDate = OffsetDateTime.now(ZoneOffset.UTC);
    LocalDateTime hearingDate = LocalDateTime.now(ZoneOffset.UTC);

    int fileSize = 1;
    for (int transformedMediaCount = 0; transformedMediaCount < count; transformedMediaCount++) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        UserAccountEntity owner = userAccountStub.createSystemUserAccount(
            TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryString(Integer.toString(transformedMediaCount)));
        UserAccountEntity requestedBy = userAccountStub.createSystemUserAccount(
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(transformedMediaCount)));

        String courtName = TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(transformedMediaCount));
        String caseNumber = CASE_NUMBER_PREFIX + transformedMediaCount;


        retTransformerMediaLst.add(createTransformedMediaEntity(mediaRequest, fileName, null, requestedDate, fileFormat, fileSize));
        fileSize = fileSize + 1;
        hoursBefore = hoursBefore.minusHours(1);
        hoursAfter = hoursAfter.plusHours(1);
        hearingDate = hearingDate.plusDays(count);
        requestedDate = requestedDate.plusDays(1);

    }
    return retTransformerMediaLst;
}

public List<Integer> getExpectedStartingFrom(int startingFromIndex, List<TransformedMediaEntity> generatedMediaEntities) {
    List<Integer> fndMediaIds = new ArrayList<>();
    for (int position = 0; position < generatedMediaEntities.size(); position++) {
        if (position >= startingFromIndex) {
            fndMediaIds.add(generatedMediaEntities.get(position).getId());
        }
    }

    return fndMediaIds;
}

public List<Integer> getExpectedTo(int toIndex, List<TransformedMediaEntity> generatedMediaEntities) {
    List<Integer> fndMediaIds = new ArrayList<>();
    for (int position = 0; position < generatedMediaEntities.size(); position++) {
        if (position <= toIndex) {
            fndMediaIds.add(generatedMediaEntities.get(position).getId());
        }
    }

    return fndMediaIds;
}

public List<Integer> getTransformedMediaIds(List<TransformedMediaEntity> entities) {
    return entities.stream().map(e -> e.getId()).collect(Collectors.toList());
}

}