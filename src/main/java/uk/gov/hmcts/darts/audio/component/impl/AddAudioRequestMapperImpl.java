package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserIdentity userIdentity;

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
        media.setCaseNumberList(addAudioMetadataRequest.getCases());
        media.setMediaFormat(addAudioMetadataRequest.getFormat());
        media.setFileSize(addAudioMetadataRequest.getFileSize());
        media.setChecksum(addAudioMetadataRequest.getChecksum());
        media.setMediaFile(addAudioMetadataRequest.getFilename());
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        media.setCreatedBy(userIdentity.getUserAccount());
        media.setLastModifiedBy(userIdentity.getUserAccount());
        return media;
    }
}