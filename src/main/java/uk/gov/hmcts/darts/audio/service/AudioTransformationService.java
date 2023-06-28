package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

public interface AudioTransformationService {

    MediaRequestEntity processAudioRequest(Integer requestId);

}
