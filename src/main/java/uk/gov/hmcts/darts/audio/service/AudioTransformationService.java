package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequest;

public interface AudioTransformationService {

    MediaRequest processAudioRequest(Integer requestId);

}
