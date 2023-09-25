package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AddAudioMetaDataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

public interface AddAudioRequestMapper {

    MediaEntity mapToMedia(AddAudioMetaDataRequest addAudioRequest);
}
