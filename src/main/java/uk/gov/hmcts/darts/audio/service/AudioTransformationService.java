package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface AudioTransformationService {

    UUID processAudioRequest(Integer requestId) throws ExecutionException, InterruptedException;

    BinaryData getAudioBlobData(UUID location);

    UUID saveAudioBlobData(BinaryData binaryData);

    TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest, UUID externalLocation);

    List<MediaEntity> getMediaMetadata(Integer hearingId);

    Optional<UUID> getMediaLocation(MediaEntity media);

    Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

    UUID saveProcessedData(MediaRequestEntity mediaRequest, BinaryData binaryData);

    Path saveMediaToWorkspace(MediaEntity mediaEntity) throws IOException;
}
