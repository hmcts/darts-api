package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;

import java.time.OffsetDateTime;

public interface AudioLoggerService {
    void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest);

    void addAudioSmallFileWithLongDuration(String courthouse, String courtroom, OffsetDateTime startDate, OffsetDateTime finishDate,
                                           Integer medId, Long fileSize);
}
