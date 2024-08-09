package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Component
@RequiredArgsConstructor
public class TranscriptionStubComposable {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;

    public TranscriptionEntity createTranscription(UserAccountStubComposable userAccountStubComposable, HearingEntity hearing) {
        return createTranscription(userAccountStubComposable, hearing,null);
    }

    public TranscriptionEntity createTranscription(
        UserAccountStubComposable userAccountStubComposable, HearingEntity hearing, UserAccountEntity authorisedIntegrationTestUser) {

        TranscriptionTypeEntity transcriptionType = mapToTranscriptionTypeEntity(SENTENCING_REMARKS);
        TranscriptionStatusEntity transcriptionStatus = mapToTranscriptionStatusEntity(APPROVED);
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = mapToTranscriptionUrgencyEntity(STANDARD);
        UserAccountEntity userAccountToApply = authorisedIntegrationTestUser;
        if (userAccountToApply == null) {
            userAccountToApply = userAccountStubComposable.createAuthorisedIntegrationTestUser(hearing.getCourtCase().getCourthouse());
        }
        return createAndSaveTranscriptionEntity(
            hearing,
            transcriptionType,
            transcriptionStatus,
            Optional.of(transcriptionUrgencyEntity),
            userAccountToApply
        );
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

        if (hearing != null) {
            hearing.getTranscriptions().add(transcription);
        }

        return transcriptionRepository.saveAndFlush(transcription);
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
}