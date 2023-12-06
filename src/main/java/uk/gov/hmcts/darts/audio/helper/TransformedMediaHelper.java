package uk.gov.hmcts.darts.audio.helper;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.enums.DatastoreContainerType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.TRANSFORMED_MEDIA_ID;

@Component
@RequiredArgsConstructor
public class TransformedMediaHelper {
    private final TransientObjectDirectoryService transientObjectDirectoryService;
    private final TransformedMediaRepository transformedMediaRepository;
    private final DataManagementApi dataManagementApi;


    @Transactional
    public UUID saveToStorage(MediaRequestEntity mediaRequest, BinaryData binaryData, String filename) {

        //save in outbound datastore
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MEDIA_REQUEST_ID, String.valueOf(mediaRequest.getId()));
        BlobClient blobClient = dataManagementApi.saveBlobDataToContainer(binaryData, DatastoreContainerType.OUTBOUND, metadata);

        //save in database
        TransformedMediaEntity transformedMediaEntity = createTransformedMediaEntity(mediaRequest, filename);
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientObjectDirectoryEntity(
            transformedMediaEntity,
            blobClient
        );


        Map<String, String> newMetadata = new HashMap<>();
        metadata.put(TRANSFORMED_MEDIA_ID, String.valueOf(transientObjectDirectoryEntity.getTransformedMedia().getId()));
        dataManagementApi.addMetadata(blobClient, newMetadata);
        return UUID.fromString(blobClient.getBlobName());
    }

    private TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequest, String filename) {
        TransformedMediaEntity entity = new TransformedMediaEntity();
        entity.setMediaRequest(mediaRequest);
        entity.setOutputFilename(filename);
        entity.setStartTime(mediaRequest.getStartTime());
        entity.setEndTime(mediaRequest.getEndTime());
        entity.setCreatedBy(mediaRequest.getCreatedBy());
        entity.setLastModifiedBy(mediaRequest.getCreatedBy());
        transformedMediaRepository.save(entity);
        return entity;
    }

}
