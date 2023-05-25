package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioRequestService;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
@Slf4j
public class AudioRequestServiceImpl implements AudioRequestService {

    private final AudioRequestRepository audioRequestRepository;

    @Override
    public Integer saveAudioRequest(AudioRequestDetails request) {

        var audioRequest = saveAudioRequestToDb(request.getCaseId(),
                             request.getRequester(),
                             request.getStartTime(),
                             request.getEndTime(),
                             request.getRequestType());

        return audioRequest.getRequestId();
    }

    private AudioRequest saveAudioRequestToDb(String caseId, String requester,
                                              OffsetDateTime startTime, OffsetDateTime endTime,
                                              String requestType) {

        AudioRequest audioRequest = new AudioRequest();
        audioRequest.setCaseId(caseId);
        audioRequest.setRequester(requester);
        audioRequest.setStartTime(Timestamp.valueOf(startTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
        audioRequest.setEndTime(Timestamp.valueOf(endTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
        audioRequest.setRequestType(requestType);
        audioRequest.setStatus(String.valueOf(AudioRequestStatus.OPEN));
        audioRequest.setAttempts(0);

        return audioRequestRepository.saveAndFlush(audioRequest);
    }
}
