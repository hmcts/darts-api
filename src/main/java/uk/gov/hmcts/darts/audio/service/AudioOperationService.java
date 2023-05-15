package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AudioOperationService {

    AudioFileInfo concatenate(String workspaceDir, List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException;

    AudioFileInfo merge(List<AudioFileInfo> audioFilesInfo, String workspaceDir)
        throws ExecutionException, InterruptedException;

    AudioFileInfo trim(String workspaceDir, AudioFileInfo audioFileInfo, String startTime, String endTime)
        throws ExecutionException, InterruptedException;

    AudioFileInfo reEncode(String workspaceDir, AudioFileInfo audioFileInfo)
        throws ExecutionException, InterruptedException;

}
