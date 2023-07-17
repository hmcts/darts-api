package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface OutboundFileProcessor {

    List<List<AudioFileInfo>> processAudio(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                           OffsetDateTime overallStartTime,
                                           OffsetDateTime overallEndTime)
        throws ExecutionException, InterruptedException;

}
