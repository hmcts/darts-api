package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface OutboundFileZipGenerator {
    Path generateAndWriteZip(List<List<AudioFileInfo>> audioSessions, MediaRequestEntity mediaRequestEntity);
}
