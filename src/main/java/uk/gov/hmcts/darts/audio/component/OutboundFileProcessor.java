package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface OutboundFileProcessor {

    List<List<AudioFileInfo>> processAudioForDownload(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                      OffsetDateTime overallStartTime,
                                                      OffsetDateTime overallEndTime)
        throws ExecutionException, InterruptedException, IOException;

    List<AudioFileInfo> processAudioForPlaybacks(Map<MediaEntity, Path> mediaEntityToDownloadLocation, OffsetDateTime startTime,
                                          OffsetDateTime endTime)
        throws ExecutionException, InterruptedException, IOException;


}
