package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final CourtroomRepository courtroomRepository;

    public MediaEntity mapToMedia(AddAudioMetadataRequest addAudioMetadataRequest) {
        MediaEntity media = new MediaEntity();
        media.setStart(addAudioMetadataRequest.getStartedAt());
        media.setEnd(addAudioMetadataRequest.getEndedAt());
        media.setChannel(addAudioMetadataRequest.getChannel());
        media.setTotalChannels(addAudioMetadataRequest.getTotalChannels());
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            addAudioMetadataRequest.getCourthouse(),
            addAudioMetadataRequest.getCourtroom()
        );
        foundCourtroom.ifPresent(media::setCourtroom);
        media.setCaseIdList(addAudioMetadataRequest.getCases());
        return media;
    }
}
