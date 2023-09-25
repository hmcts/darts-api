package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetaDataRequest;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final CourtroomRepository courtroomRepository;

    public MediaEntity mapToMedia(AddAudioMetaDataRequest addAudioMetaDataRequest) {
        MediaEntity media = new MediaEntity();
        media.setStart(addAudioMetaDataRequest.getStartedAt());
        media.setEnd(addAudioMetaDataRequest.getEndedAt());
        media.setChannel(addAudioMetaDataRequest.getChannel());
        media.setTotalChannels(addAudioMetaDataRequest.getTotalChannels());
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            addAudioMetaDataRequest.getCourthouse(),
            addAudioMetaDataRequest.getCourtroom()
        );
        foundCourtroom.ifPresent(media::setCourtroom);
        media.setCaseIdList(addAudioMetaDataRequest.getCases());
        return media;
    }
}
