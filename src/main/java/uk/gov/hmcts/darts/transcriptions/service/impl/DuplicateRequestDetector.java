package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DuplicateRequestDetector {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;

    public void checkForDuplicate(TranscriptionRequestDetails requestDetails, boolean isManual) {
        var ignoreStatusList = List.of(new TranscriptionStatusEntity(TranscriptionStatusEnum.REJECTED.getId()),
                                       new TranscriptionStatusEntity(TranscriptionStatusEnum.CLOSED.getId()));
        List<TranscriptionEntity> matchingTranscriptions = transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            requestDetails.getHearingId(),
            getTranscriptionTypeById(requestDetails.getTranscriptionTypeId()),
            requestDetails.getStartDateTime(),
            requestDetails.getEndDateTime(),
            isManual,
            ignoreStatusList
        );
        if (!matchingTranscriptions.isEmpty()) {
            var duplicateTranscription = matchingTranscriptions.getFirst();
            if (duplicateTranscription.getTranscriptionStatus().getId().equals(TranscriptionStatusEnum.COMPLETE.getId())) {
                throw new DartsApiException(
                    TranscriptionApiError.DUPLICATE_TRANSCRIPTION,
                    Collections.singletonMap("duplicate_transcription_id", duplicateTranscription.getId())
                );
            } else {
                throw new DartsApiException(TranscriptionApiError.DUPLICATE_TRANSCRIPTION);
            }
        }
    }

    private TranscriptionTypeEntity getTranscriptionTypeById(Integer transcriptionTypeId) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeId);
    }
}
