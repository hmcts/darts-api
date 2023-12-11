package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AudioTransformationService {

    BinaryData getUnstructuredAudioBlob(UUID location);

    BinaryData getOutboundAudioBlob(UUID location);

    UUID saveAudioBlobData(BinaryData binaryData);

    List<MediaEntity> getMediaMetadata(Integer hearingId);

    Optional<UUID> getMediaLocation(MediaEntity media);

    Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

    Path saveMediaToWorkspace(MediaEntity mediaEntity) throws IOException;

    void handleKedaInvocationForMediaRequests();

}
