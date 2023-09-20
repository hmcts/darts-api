package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioResponse;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

public interface AudioResponseMapper {

    List<AudioMetadata> mapToAudioMetadata(List<MediaEntity> mediaEntities);

    AddAudioResponse mapToAddAudioResponse(MediaRequestEntity audioRequest);
}
