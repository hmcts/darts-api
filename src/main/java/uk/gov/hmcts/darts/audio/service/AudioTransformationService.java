package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

public interface AudioTransformationService {

    List<MediaEntity> getMediaByHearingId(Integer hearingId);

    Path saveBlobDataToTempWorkspace(InputStream mediaFile, String fileName) throws IOException;

    List<MediaEntity> filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(List<MediaEntity> mediaEntitiesForRequest,
                                                                                     OffsetDateTime startTime,
                                                                                     OffsetDateTime endTime);

    Path retrieveFromStorageAndSaveToTempWorkspace(MediaEntity mediaEntity) throws IOException;

    void handleKedaInvocationForMediaRequests();

}