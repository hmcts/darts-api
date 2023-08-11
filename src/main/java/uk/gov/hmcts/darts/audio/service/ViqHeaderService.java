package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;

import java.time.OffsetDateTime;
import java.util.Set;

public interface ViqHeaderService {

    String generatePlaylist(Set<PlaylistInfo> playlistInfos, String outputFileLocation);

    String generateAnnotation(Integer hearingId, OffsetDateTime startTime, OffsetDateTime endTime,
                              String outputFileLocation);

    String generateReadme(ViqMetaData viqMetaData, String fileLocation);
}
