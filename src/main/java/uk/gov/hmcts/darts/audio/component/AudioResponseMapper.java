package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@FunctionalInterface
public interface AudioResponseMapper {
    List<AudioMetadata> mapToAudioMetadata(List<MediaEntity> mediaEntities);
}
