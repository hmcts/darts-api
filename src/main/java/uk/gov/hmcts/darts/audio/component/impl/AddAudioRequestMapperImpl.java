package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.util.List;

import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;

@RequiredArgsConstructor
@Component
public class AddAudioRequestMapperImpl implements AddAudioRequestMapper {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserIdentity userIdentity;
    private final MediaLinkedCaseHelper mediaLinkedCaseHelper;
    private final MediaRepository mediaRepository;

    @Override
    public MediaEntity mapToMedia(AddAudioMetadataRequest addAudioMetadataRequest, UserAccountEntity userAccount) {
        MediaEntity media = new MediaEntity();
        media.setStart(addAudioMetadataRequest.getStartedAt());
        media.setEnd(addAudioMetadataRequest.getEndedAt());
        media.setChannel(addAudioMetadataRequest.getChannel());
        media.setTotalChannels(addAudioMetadataRequest.getTotalChannels());
        CourtroomEntity foundCourtroom = retrieveCoreObjectService.retrieveOrCreateCourtroom(
            addAudioMetadataRequest.getCourthouse(),
            addAudioMetadataRequest.getCourtroom(),
            userAccount
        );
        media.setCourtroom(foundCourtroom);
        media.setMediaFormat(addAudioMetadataRequest.getFormat());
        media.setFileSize(addAudioMetadataRequest.getFileSize());
        media.setChecksum(addAudioMetadataRequest.getChecksum());
        media.setMediaFile(addAudioMetadataRequest.getFilename());
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        media.setCreatedBy(userIdentity.getUserAccount());
        media.setLastModifiedBy(userIdentity.getUserAccount());

        mediaRepository.saveAndFlush(media);
        addCasesToMedia(media, foundCourtroom.getCourthouse(), addAudioMetadataRequest.getCases(), userAccount);

        return media;
    }

    private void addCasesToMedia(MediaEntity media, CourthouseEntity courthouse, List<String> caseNumbers, UserAccountEntity userAccount) {
        for (String caseNumber : caseNumbers) {
            CourtCaseEntity courtCase = retrieveCoreObjectService.retrieveOrCreateCase(
                courthouse,
                caseNumber,
                userAccount
            );
            mediaLinkedCaseHelper.addCase(media, courtCase, MediaLinkedCaseSourceType.ADD_AUDIO_METADATA, userAccount);
        }

    }
}