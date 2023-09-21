package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audiorecording.model.AddAudioRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

public interface AddAudioRequestMapper {

    MediaEntity mapToMedia(AddAudioRequest addAudioRequest);
}
