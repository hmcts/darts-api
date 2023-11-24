package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
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
        if (requestDetails.getStartDateTime() != null && requestDetails.getEndDateTime() != null) {

            List<TranscriptionEntity> matchingTranscriptions = transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManual(
                requestDetails.getHearingId(),
                getTranscriptionTypeById(requestDetails.getTranscriptionTypeId()),
                requestDetails.getStartDateTime(),
                requestDetails.getEndDateTime(),
                isManual
            );
            if (!matchingTranscriptions.isEmpty()) {
                var duplicateTranscriptionIds = matchingTranscriptions.get(0).getId();
                throw new DartsApiException(
                    TranscriptionApiError.DUPLICATE_TRANSCRIPTION,
                    Collections.singletonMap("duplicate_transcription_id", duplicateTranscriptionIds)
                );
            }
        }
    }

    private TranscriptionTypeEntity getTranscriptionTypeById(Integer transcriptionTypeId) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeId);
    }
}
