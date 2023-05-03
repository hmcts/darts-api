package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.io.IOException;
import java.util.List;

public interface AudioOperationService {

    AudioFileInfo concatenate(String workspaceDir, List<AudioFileInfo> audioFileInfos) throws IOException;
}
