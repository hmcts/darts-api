package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.dets.api.impl.DetsDataManagementApiImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArmDataManagementApiImpl implements ArmDataManagementApi {

    private final ArmService armService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmApiService armApiService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final DataManagementService dataManagementService;
    private final DetsDataManagementApiImpl detsDataManagementApi;

    @Override
    public String saveBlobDataToArm(String filename, BinaryData binaryData) {
        return armService.saveBlobData(armDataManagementConfiguration.getContainerName(), filename, binaryData);
    }

    @Override
    public void copyBlobDataToArm(String unstructuredUuid, String filename) {

        String blobPathAndName = armDataManagementConfiguration.getFolders().getSubmission() + filename;
        dataManagementService.copyBlobData(
            dataManagementConfiguration.getUnstructuredContainerName(), armDataManagementConfiguration.getContainerName(), unstructuredUuid, blobPathAndName);
    }

    public void copyDetsBlobDataToArm(String detsUuid, String filename) {

        String blobPathAndName = armDataManagementConfiguration.getFolders().getSubmission() + filename;
        detsDataManagementApi.copyDetsBlobDataToArm(detsUuid, blobPathAndName);
    }

    @Override
    public List<String> listResponseBlobs(String prefix) {
        return armService.listResponseBlobs(armDataManagementConfiguration.getContainerName(), prefix);
    }

    @Override
    public List<String> listResponseBlobsUsingBatch(String prefix, int batchSize) {
        return armService.listResponseBlobsUsingBatch(armDataManagementConfiguration.getContainerName(),
                                                      prefix,
                                                      batchSize);
    }

    @Override
    public ContinuationTokenBlobs listResponseBlobsUsingMarker(String prefix, int batchSize, String continuationToken) {
        return armService.listResponseBlobsWithMarker(armDataManagementConfiguration.getContainerName(),
                                                      prefix,
                                                      batchSize,
                                                      continuationToken);
    }

    @Override
    public BinaryData getBlobData(String blobPathAndName) {
        return armService.getBlobData(
            armDataManagementConfiguration.getContainerName(),
            blobPathAndName
        );
    }

    @Override
    public boolean deleteBlobData(String blobPathAndName) {
        return armService.deleteBlobData(armDataManagementConfiguration.getContainerName(), blobPathAndName);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp, int retConfScore, String retConfReason) {
        return armApiService.updateMetadata(externalRecordId, eventTimestamp, retConfScore, retConfReason);
    }

    @Override
    public DownloadResponseMetaData downloadBlobFromContainer(DatastoreContainerType container,
                                                              ExternalObjectDirectoryEntity blobId) throws FileNotDownloadedException {
        Optional<String> containerName = getContainerName(container);
        if (containerName.isPresent()) {
            return armApiService.downloadArmData(blobId.getExternalRecordId(), blobId.getExternalFileId());
        }
        throw new FileNotDownloadedException("Container for " + container.name() + " not found in ARM.");

    }

    @Override
    public Optional<String> getContainerName(DatastoreContainerType datastoreContainerType) {
        if (Objects.requireNonNull(datastoreContainerType) == DatastoreContainerType.ARM) {
            return Optional.of(armDataManagementConfiguration.getContainerName());
        }
        return Optional.empty();
    }

    @Override
    public StorageConfiguration getConfiguration() {
        return armDataManagementConfiguration;
    }
}