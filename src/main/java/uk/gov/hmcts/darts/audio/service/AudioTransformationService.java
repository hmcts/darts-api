package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import java.io.IOException;
import java.nio.file.Path;

public interface AudioTransformationService {

    MediaRequestEntity processAudioRequest(Integer requestId);

    Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

}
