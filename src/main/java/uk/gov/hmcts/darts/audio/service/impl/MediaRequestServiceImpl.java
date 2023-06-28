package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestType;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;

@RequiredArgsConstructor
@Service
public class MediaRequestServiceImpl implements MediaRequestService {

    private final MediaRequestRepository mediaRequestRepository;

    @Override
    public MediaRequestEntity getMediaRequestById(Integer id) {
        return mediaRequestRepository.findById(id).orElseThrow();
    }

    @Transactional
    @Override
    public MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus status) {
        MediaRequestEntity mediaRequestEntity = getMediaRequestById(id);
        mediaRequestEntity.setStatus(status);

        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

    @Transactional
    @Override
    public Integer saveAudioRequest(AudioRequestDetails request) {

        var audioRequest = saveAudioRequestToDb(
            request.getHearingId(),
            request.getRequestor(),
            request.getStartTime(),
            request.getEndTime(),
            request.getRequestType()
        );

        return audioRequest.getRequestId();
    }

    private MediaRequestEntity saveAudioRequestToDb(Integer hearingId, Integer requestor,
                                                    OffsetDateTime startTime, OffsetDateTime endTime,
                                                    AudioRequestType requestType) {

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearingId(hearingId);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setRequestType(requestType);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setAttempts(0);

        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

}
