package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.ResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArmDataManagementApiImpl implements ArmDataManagementApi {

    private final ArmService armService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmApiService armApiService;

    @Override
    public String saveBlobDataToArm(String filename, BinaryData binaryData) {
        return armService.saveBlobData(armDataManagementConfiguration.getContainerName(), filename, binaryData);
    }

    @Override
    public List<String> listCollectedBlobs(String prefix) {
        return armService.listCollectedBlobs(armDataManagementConfiguration.getContainerName(), prefix);
    }

    @Override
    public List<String> listResponseBlobs(String prefix) {
        return armService.listResponseBlobs(armDataManagementConfiguration.getContainerName(), prefix);
    }

    public BinaryData getBlobData(String blobPathAndName) {
        return armService.getBlobData(
                armDataManagementConfiguration.getContainerName(),
            blobPathAndName
        );
    }

    @Override
    public void deleteResponseBlob(String blobName) {
        armService.deleteResponseBlob(armDataManagementConfiguration.getContainerName(), blobName);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp) {
        return armApiService.updateMetadata(externalRecordId, eventTimestamp);
    }

    @Override
    public boolean downloadBlobFromContainer(DatastoreContainerType container, ExternalObjectDirectoryEntity blobId, ResponseMetaData response) {
        Optional<String> containerName = getContainerName(container);
        if (containerName.isPresent()) {
            InputStream stream = armApiService.downloadArmData(blobId.getExternalRecordId(), blobId.getExternalFileId());
            response.markInputStream(stream);
            response.markSuccess();
        }
        return response.isSuccessfullyDownloaded();
    }

    public Optional<String> getContainerName(DatastoreContainerType datastoreContainerType) {
        switch (datastoreContainerType) {
            case ARM -> {
                return Optional.of(armDataManagementConfiguration.getContainerName());
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}