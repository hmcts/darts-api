package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AudioOperationService {

    List<AudioFileInfo> concatenateWithGaps(String workspaceDir, List<AudioFileInfo> audioFileInfos, Duration allowableAudioGap)
        throws ExecutionException, InterruptedException, IOException;

    AudioFileInfo concatenate(String workspaceDir, List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException;

    AudioFileInfo merge(List<AudioFileInfo> audioFilesInfo, String workspaceDir)
        throws ExecutionException, InterruptedException, IOException;

    AudioFileInfo trim(String workspaceDir, AudioFileInfo audioFileInfo, Duration startDuration, Duration endDuration)
        throws ExecutionException, InterruptedException, IOException;

    AudioFileInfo reEncode(String workspaceDir, AudioFileInfo audioFileInfo)
        throws ExecutionException, InterruptedException, IOException;

}
