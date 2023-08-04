package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;

import java.util.List;

public interface ViqHeaderService {

    String generatePlaylist(List<PlaylistInfo> playlistInfos, String outputFileLocation);

    String generateAnnotation(Integer hearingId, String startTime, String endTime);

    String generateReadme(ViqMetaData viqMetaData, String fileLocation);
}
