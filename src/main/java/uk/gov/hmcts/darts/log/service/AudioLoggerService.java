package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;

public interface AudioLoggerService {
    void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest);
}
