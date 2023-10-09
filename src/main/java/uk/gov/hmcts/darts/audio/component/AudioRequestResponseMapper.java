package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;

public interface AudioRequestResponseMapper {
    AddAudioResponse mapToAddAudioResponse(MediaRequestEntity audioRequest);
}
