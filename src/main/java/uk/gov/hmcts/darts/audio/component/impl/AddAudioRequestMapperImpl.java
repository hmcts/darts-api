package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final RetrieveCoreObjectService retrieveCoreObjectService;

    public MediaEntity mapToMedia(AddAudioMetadataRequest addAudioMetadataRequest) {
        MediaEntity media = new MediaEntity();
        media.setStart(addAudioMetadataRequest.getStartedAt());
        media.setEnd(addAudioMetadataRequest.getEndedAt());
        media.setChannel(addAudioMetadataRequest.getChannel());
        media.setTotalChannels(addAudioMetadataRequest.getTotalChannels());
        CourtroomEntity foundCourtroom = retrieveCoreObjectService.retrieveOrCreateCourtroom(
            addAudioMetadataRequest.getCourthouse(),
            addAudioMetadataRequest.getCourtroom()
        );
        media.setCourtroom(foundCourtroom);
        media.setCaseIdList(addAudioMetadataRequest.getCases());
        media.setMediaFormat(addAudioMetadataRequest.getFormat());
        media.setFileSize(addAudioMetadataRequest.getFileSize());
        media.setChecksum(addAudioMetadataRequest.getChecksum());
        media.setMediaFile(addAudioMetadataRequest.getFilename());
        return media;
    }
}
