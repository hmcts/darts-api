package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.dto.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioRequestService;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
public class AudioRequestServiceImpl implements AudioRequestService {

    private final AudioRequestRepository audioRequestRepository;

    @Override
    public void saveAudioRequest(AudioRequestDetails request) {

        saveAudioRequestToDb(request.getCaseId(),
                             request.getEmailAddress(),
                             request.getStartTime(),
                             request.getEndTime());
    }

    private AudioRequest saveAudioRequestToDb(String caseId, String emailAddress, LocalDateTime startTime, LocalDateTime endTime) {

        AudioRequest audioRequest = new AudioRequest();
        audioRequest.setCaseId(caseId);
        audioRequest.setEmailAddress(emailAddress);
        audioRequest.setStartTime(Timestamp.valueOf(startTime));
        audioRequest.setEndTime(Timestamp.valueOf(endTime));
        audioRequest.setStatus(String.valueOf(AudioRequestStatus.OPEN));
        audioRequest.setAttempts(0);

        return audioRequestRepository.saveAndFlush(audioRequest);
    }
}
