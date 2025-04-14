package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AudioAsyncServiceImpl implements AudioAsyncService {

    private final AudioConfigurationProperties audioConfigurationProperties;
    private final CourtLogEventRepository courtLogEventRepository;
    private final HearingRepository hearingRepository;
    private final MediaLinkedCaseHelper mediaLinkedCaseHelper;

    @Async
    @Transactional
    @Override
    public void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia, UserAccountEntity userAccount) {

        if (addAudioMetadataRequest.getTotalChannels() == 1
            && audioConfigurationProperties.getHandheldAudioCourtroomNumbers().contains(addAudioMetadataRequest.getCourtroom())) {
            return;
        }

        String courthouse = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(addAudioMetadataRequest.getCourthouse()));
        String courtroom = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(addAudioMetadataRequest.getCourtroom()));
        OffsetDateTime start = addAudioMetadataRequest.getStartedAt().minus(audioConfigurationProperties.getPreAmbleDuration());
        OffsetDateTime end = addAudioMetadataRequest.getEndedAt().plus(audioConfigurationProperties.getPostAmbleDuration());
        List<EventEntity> courtLogs = courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            courthouse,
            courtroom,
            start,
            end
        );

        var associatedHearings = courtLogs.stream()
            .flatMap(h -> h.getHearingEntities().stream())
            .distinct()
            .toList();

        for (var hearing : associatedHearings) {
            mediaLinkedCaseHelper.linkMediaToCase(savedMedia, hearing.getCourtCase(), MediaLinkedCaseSourceType.ADD_AUDIO_EVENT_LINKING, userAccount);
            List<Integer> mediaIdList = hearing.getMedias().stream().map(MediaEntity::getId).toList();
            if (!mediaIdList.contains(savedMedia.getId())) {
                hearing.addMedia(savedMedia);
                hearing.setHearingIsActual(true);
                hearingRepository.saveAndFlush(hearing);
            }
        }
    }
}
