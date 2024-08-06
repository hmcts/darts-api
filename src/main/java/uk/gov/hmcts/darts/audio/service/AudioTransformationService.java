package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface AudioTransformationService {

    List<MediaEntity> getMediaMetadata(Integer hearingId);

    Path saveBlobDataToTempWorkspace(InputStream mediaFile, String fileName) throws IOException;

    Path retrieveFromStorageAndSaveToTempWorkspace(MediaEntity mediaEntity) throws IOException;

    void handleKedaInvocationForMediaRequests();

}