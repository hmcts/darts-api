package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.ZonedDateTime;
import java.util.Set;

public interface ViqHeaderService {

    String generatePlaylist(Set<PlaylistInfo> playlistInfos, String outputFileLocation);

    String generateAnnotation(HearingEntity hearingEntity, ZonedDateTime startTime, ZonedDateTime endTime,
                              String annotationsOutputFile);

    String generateReadme(ViqMetaData viqMetaData, String fileLocation);
}
