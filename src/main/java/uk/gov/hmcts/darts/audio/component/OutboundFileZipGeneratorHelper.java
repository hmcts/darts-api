package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqHeader;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Set;

public interface OutboundFileZipGeneratorHelper {

    String generatePlaylist(Set<PlaylistInfo> playlistInfos, String outputFileLocation);

    String generateAnnotation(HearingEntity hearingEntity, ZonedDateTime startTime, ZonedDateTime endTime,
                              String annotationsOutputFile);

    String generateReadme(ViqMetaData viqMetaData, String fileLocation);

    Path generateViqFile(ViqHeader viqHeader, AudioFileInfo audioFileInfo);
}
