package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AudioTransformationService {

    List<MediaEntity> getMediaMetadata(Integer hearingId);

    Optional<UUID> getMediaLocation(MediaEntity media, Integer containerLocationId);

    Path saveBlobDataToTempWorkspace(InputStream mediaFile, String fileName) throws IOException;

    Path retrieveFromStorageAndSaveToTempWorkspace(MediaEntity mediaEntity) throws IOException;

    void handleKedaInvocationForMediaRequests();

}
