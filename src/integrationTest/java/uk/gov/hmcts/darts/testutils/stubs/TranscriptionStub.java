package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Component
@RequiredArgsConstructor
public class TranscriptionStub {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;
    private final HearingStub hearingStub;

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
        return transcriptionDocumentEntity;
    }

    public TranscriptionEntity createMinimalTranscription() {
        return createTranscription(hearingStub.createMinimalHearing());
    }

    public TranscriptionEntity createTranscription(
          HearingEntity hearing
    ) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);
        UserAccountEntity authorisedIntegrationTestUser = userAccountStub.createAuthorisedIntegrationTestUser(hearing.getCourtCase()
              .getCourthouse());
        return createAndSaveTranscriptionEntity(
              hearing,
              transcriptionType,
              transcriptionStatus,
              transcriptionUrgencyEntity,
              authorisedIntegrationTestUser
        );
    }

    public TranscriptionEntity createTranscription(
          CourtCaseEntity courtCase
    ) {
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
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = new TranscriptionUrgencyEntity();
        transcriptionUrgencyEntity.setId(urgencyEnum.getId());
        transcriptionUrgencyEntity.setDescription(urgencyEnum.name());
        return transcriptionUrgencyEntity;
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
          TranscriptionUrgencyEntity transcriptionUrgency,
          UserAccountEntity testUser) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setCourtroom(hearing.getCourtroom());
        transcription.addHearing(hearing);
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setCreatedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManualTranscription(true);
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
              getTranscriptionStatusByEnum(AWAITING_AUTHORISATION)
        );
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
              getTranscriptionStatusByEnum(COMPLETE)
        );
        transcriptionEntity.setHideRequestFromRequestor(hideRequestFromRequester);
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
    }

    public TranscriptionWorkflowEntity createTranscriptionWorkflowEntity(TranscriptionEntity transcriptionEntity,
          UserAccountEntity user,
          OffsetDateTime timestamp,
          TranscriptionStatusEntity transcriptionStatus) {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = new TranscriptionWorkflowEntity();
        transcriptionWorkflowEntity.setTranscription(transcriptionEntity);
        transcriptionWorkflowEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionWorkflowEntity.setWorkflowActor(user);
        transcriptionWorkflowEntity.setWorkflowTimestamp(timestamp);
        return transcriptionWorkflowEntity;
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
              checksum
        );

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
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryEntity);

        transcriptionDocumentEntity.getExternalObjectDirectoryEntities().add(externalObjectDirectoryEntity);

        transcriptionEntity.getTranscriptionDocumentEntities().add(transcriptionDocumentEntity);
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
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
          TranscriptionStatusEntity status) {
        final var transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.addCase(courtCaseEntity);
        transcriptionEntity.addHearing(hearingEntity);
        transcriptionEntity.setTranscriptionType(getTranscriptionTypeByEnum(SPECIFIED_TIMES));
        transcriptionEntity.setTranscriptionUrgency(getTranscriptionUrgencyByEnum(STANDARD));
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

        final var requestedTranscriptionWorkflowEntity = createTranscriptionWorkflowEntity(
              transcriptionEntity,
              userAccountEntity,
              workflowTimestamp,
              getTranscriptionStatusByEnum(REQUESTED)
        );

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = createTranscriptionWorkflowEntity(
              transcriptionEntity,
              userAccountEntity,
              workflowTimestamp,
              status
        );

        transcriptionEntity.getTranscriptionWorkflowEntities()
              .addAll(List.of(requestedTranscriptionWorkflowEntity, transcriptionWorkflowEntity));
        return transcriptionRepository.saveAndFlush(transcriptionEntity);
    }
}
