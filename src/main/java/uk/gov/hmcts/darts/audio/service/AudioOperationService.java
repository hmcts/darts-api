package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AudioOperationService {

    AudioFileInfo concatenate(String workspaceDir, List<AudioFileInfo> audioFileInfos) throws ExecutionException, InterruptedException;
}
