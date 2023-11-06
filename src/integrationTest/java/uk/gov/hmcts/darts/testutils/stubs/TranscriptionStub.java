package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
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
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
public class TranscriptionStub {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;


    public TranscriptionEntity createTranscription(
        HearingEntity hearing
    ) {
        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(TranscriptionTypeEnum.SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(TranscriptionStatusEnum.APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(TranscriptionUrgencyEnum.STANDARD);
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
        return transcriptionStatus;
    }

    public TranscriptionEntity createAndSaveTranscriptionEntity(HearingEntity hearing,
                                                                TranscriptionTypeEntity transcriptionType,
                                                                TranscriptionStatusEntity transcriptionStatus,
                                                                TranscriptionUrgencyEntity transcriptionUrgency,
                                                                UserAccountEntity testUser) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setCourtCase(hearing.getCourtCase());
        transcription.setCourtroom(hearing.getCourtroom());
        transcription.setHearing(hearing);
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setCreatedBy(testUser);
        transcription.setLastModifiedBy(testUser);
        transcription.setIsManual(false);
        return transcriptionRepository.saveAndFlush(transcription);
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
                                                               ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                               ExternalLocationTypeEntity externalLocationTypeEntity,
                                                               UUID externalLocation,
                                                               String checksum) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setFileName(fileName);
        transcriptionDocumentEntity.setFileType(fileType);
        transcriptionDocumentEntity.setFileSize(fileSize);
        transcriptionDocumentEntity.setUploadedBy(testUser);
        transcriptionDocumentEntity.setUploadedDateTime(now(UTC));

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setStatus(objectDirectoryStatusEntity);
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(checksum);
        externalObjectDirectoryEntity.setTransferAttempts(null);
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
}
