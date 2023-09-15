package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioResponse;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@Component
public class AudioResponseMapperImpl implements AudioResponseMapper {

    @Override
    public AddAudioResponse mapToAddAudioResponse(MediaRequestEntity audioRequest) {
        AddAudioResponse addAudioResponse = new AddAudioResponse();
        addAudioResponse.setRequestId(audioRequest.getId());
        addAudioResponse.setCaseId(audioRequest.getHearing().getCourtCase().getCaseNumber());
        addAudioResponse.setCourthouseName(audioRequest.getHearing().getCourtroom().getCourthouse().getCourthouseName());
        addAudioResponse.setDefendants(audioRequest.getHearing().getCourtCase().getDefendantStringList());
        addAudioResponse.setHearingDate(audioRequest.getHearing().getHearingDate());
        addAudioResponse.setStartTime(audioRequest.getStartTime());
        addAudioResponse.setEndTime(audioRequest.getEndTime());
        return addAudioResponse;
    }

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
