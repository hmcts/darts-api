package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;

@Service
@RequiredArgsConstructor
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestRepository mediaRequestRepository;

    @Transactional
    @Override
    public MediaRequest processAudioRequest(Integer requestId) {
        MediaRequest mediaRequest = mediaRequestRepository.getReferenceById(requestId);
        mediaRequest.setStatus(PROCESSING);

        return mediaRequestRepository.saveAndFlush(mediaRequest);
    }

}
