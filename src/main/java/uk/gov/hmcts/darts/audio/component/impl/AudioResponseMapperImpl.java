package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@Component
public class AudioResponseMapperImpl implements AudioResponseMapper {

    @Override
    public List<AudioMetadata> mapToAudioMetadata(List<MediaEntity> mediaEntities) {
        return mediaEntities.stream()
            .map(this::mapToAudioMetadata)
            .toList();
    }

    private AudioMetadata mapToAudioMetadata(MediaEntity mediaEntity) {
        var audioMetadata = new AudioMetadata();

        audioMetadata.setId(mediaEntity.getId());
        audioMetadata.setMediaStartTimestamp(mediaEntity.getStart());
        audioMetadata.setMediaEndTimestamp(mediaEntity.getEnd());

        return audioMetadata;
    }

}
