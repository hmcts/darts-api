package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
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
    public MediaRequest getMediaRequestById(Integer id) {
        return mediaRequestRepository.getReferenceById(id);
    }

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

    private MediaRequest saveAudioRequestToDb(Integer hearingId, Integer requestor,
                                              OffsetDateTime startTime, OffsetDateTime endTime,
                                              AudioRequestType requestType) {

        MediaRequest mediaRequest = new MediaRequest();
        mediaRequest.setHearingId(hearingId);
        mediaRequest.setRequestor(requestor);
        mediaRequest.setStartTime(startTime);
        mediaRequest.setEndTime(endTime);
        mediaRequest.setRequestType(requestType);
        mediaRequest.setStatus(OPEN);
        mediaRequest.setAttempts(0);

        return mediaRequestRepository.saveAndFlush(mediaRequest);
    }

}
