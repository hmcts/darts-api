package uk.gov.hmcts.darts.log.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;

@Service
@Slf4j
public class AudioLoggerServiceImpl implements AudioLoggerService {

    @Override
    public void audioUploaded(AddAudioMetadataRequest request) {
        log.info("Audio uploaded: courthouse={}, courtroom={}, started_at={}, ended_at={}",
                 request.getCourthouse(),
                 request.getCourtroom(),
                 request.getStartedAt(),
                 request.getEndedAt());
    }
}
