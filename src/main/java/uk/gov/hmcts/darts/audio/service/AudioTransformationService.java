package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface AudioTransformationService {

    MediaRequestEntity processAudioRequest(Integer requestId);

    BinaryData getAudioBlobData(UUID location);

    UUID saveAudioBlobData(BinaryData binaryData);

    TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest, UUID externalLocation);

    Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

}
