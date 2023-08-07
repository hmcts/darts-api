package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;

import java.nio.file.Path;
import java.util.List;

public interface OutboundFileZipGenerator {

    Path generateAndWriteZip(List<List<AudioFileInfo>> audioSessions, MediaRequestEntity mediaRequestEntity, AudioRequestType audioRequestType);

}
