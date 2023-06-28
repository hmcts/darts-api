package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;

@Service
@RequiredArgsConstructor
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestService mediaRequestService;

    @Transactional
    @Override
    public MediaRequest processAudioRequest(Integer requestId) {

        return mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);
    }

}
