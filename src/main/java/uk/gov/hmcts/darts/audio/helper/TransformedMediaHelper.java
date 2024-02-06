package uk.gov.hmcts.darts.audio.helper;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.enums.DatastoreContainerType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.TRANSFORMED_MEDIA_ID;

@Component
@RequiredArgsConstructor
public class TransformedMediaHelper {

    private final TransientObjectDirectoryService transientObjectDirectoryService;
    private final TransformedMediaRepository transformedMediaRepository;
    private final DataManagementApi dataManagementApi;

    @Transactional
    public UUID saveToStorage(MediaRequestEntity mediaRequest, BinaryData binaryData, String filename, AudioFileInfo audioFileInfo) {

        OffsetDateTime startTime = audioFileInfo.getStartTime().atOffset(ZoneOffset.UTC);
        OffsetDateTime endTime = audioFileInfo.getEndTime().atOffset(ZoneOffset.UTC);

        //save in outbound datastore
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MEDIA_REQUEST_ID, String.valueOf(mediaRequest.getId()));
        BlobClient blobClient = dataManagementApi.saveBlobDataToContainer(binaryData, DatastoreContainerType.OUTBOUND, metadata);

        //save in database
        TransformedMediaEntity transformedMediaEntity = createTransformedMediaEntity(mediaRequest, filename, startTime, endTime, binaryData.getLength());
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientObjectDirectoryEntity(
              transformedMediaEntity,
              blobClient
        );

        dataManagementApi.addMetadata(blobClient, TRANSFORMED_MEDIA_ID, String.valueOf(transientObjectDirectoryEntity.getTransformedMedia().getId()));
        return UUID.fromString(blobClient.getBlobName());
    }

    @Transactional
    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequest, String filename,
          OffsetDateTime startTime, OffsetDateTime endTime,
          Long fileSize) {
        AudioRequestOutputFormat audioRequestOutputFormat = AudioRequestOutputFormat.MP3;
        if (mediaRequest.getRequestType().equals(DOWNLOAD)) {
            audioRequestOutputFormat = AudioRequestOutputFormat.ZIP;
        }
        TransformedMediaEntity entity = new TransformedMediaEntity();
        entity.setMediaRequest(mediaRequest);
        entity.setOutputFilename(filename);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setCreatedBy(mediaRequest.getCreatedBy());
        entity.setLastModifiedBy(mediaRequest.getCreatedBy());
        entity.setOutputFormat(audioRequestOutputFormat);
        if (nonNull(fileSize)) {
            entity.setOutputFilesize(fileSize.intValue());
        }
        transformedMediaRepository.save(entity);
        return entity;
    }
}
