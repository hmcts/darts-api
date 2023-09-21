package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audiorecording.model.AddAudioRequest;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final CourtroomRepository courtroomRepository;

    public MediaEntity mapToMedia(AddAudioRequest addAudioRequest) {
        MediaEntity media = new MediaEntity();
        media.setStart(addAudioRequest.getStartedAt());
        media.setEnd(addAudioRequest.getEndedAt());
        media.setChannel(addAudioRequest.getChannel());
        media.setTotalChannels(addAudioRequest.getTotalChannels());
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            addAudioRequest.getCourthouse(),
            addAudioRequest.getCourtroom()
        );
        foundCourtroom.ifPresent(media::setCourtroom);
        media.setCaseIdList(addAudioRequest.getCases());
        return media;
    }
}
