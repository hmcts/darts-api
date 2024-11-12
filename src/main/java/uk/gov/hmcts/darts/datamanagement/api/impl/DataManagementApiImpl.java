package uk.gov.hmcts.darts.datamanagement.api.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class DataManagementApiImpl implements DataManagementApi {

    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;

    @Override
    public DownloadResponseMetaData getBlobDataFromOutboundContainer(UUID blobId) throws FileNotDownloadedException {
        return dataManagementService.downloadData(DatastoreContainerType.OUTBOUND, getOutboundContainerName(), blobId);
    }

    @Override
    public BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata) {
        Optional<String> containerName = getContainerName(container);
        return containerName.map(s -> dataManagementService.saveBlobData(s, binaryData, metadata)).orElse(null);
    }

    @Override
    public BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container, Map<String, String> metadata) {
        String containerName = getContainerNameRequired(container);
        return dataManagementService.saveBlobData(containerName, inputStream, metadata);
    }

    @Override
    public BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container) {
        return saveBlobToContainer(inputStream, container, null);
    }

    @Override
    public void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getOutboundContainerName(), blobId);
    }

    @Override
    public void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getInboundContainerName(), blobId);
    }

    @Override
    public void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getUnstructuredContainerName(), blobId);
    }

    @Override
    public UUID saveBlobDataToInboundContainer(InputStream inputStream) {
        return dataManagementService.saveBlobData(getInboundContainerName(), inputStream).getBlobName();
    }

    /**
     * @deprecated This implementation is not memory-efficient with large files, use saveBlobDataToInboundContainer(InputStream inputStream) instead.
     */
    @Deprecated
    @Override
    public UUID saveBlobDataToInboundContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(getInboundContainerName(), binaryData);
    }

    @Override
    public UUID saveBlobDataToUnstructuredContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(getUnstructuredContainerName(), binaryData);
    }

    @Override
    public String getChecksum(DatastoreContainerType datastoreContainerType, UUID guid) {
        return dataManagementService.getChecksum(getContainerNameRequired(datastoreContainerType), guid);
    }

    private String getOutboundContainerName() {
        return dataManagementConfiguration.getOutboundContainerName();
    }

    private String getInboundContainerName() {
        return dataManagementConfiguration.getInboundContainerName();
    }

    private String getUnstructuredContainerName() {
        return dataManagementConfiguration.getUnstructuredContainerName();
    }

    @Override
    public DownloadResponseMetaData downloadBlobFromContainer(DatastoreContainerType container,
                                                              ExternalObjectDirectoryEntity externalObjectDirectoryEntity) throws FileNotDownloadedException {
        Optional<String> containerName = getContainerName(container);
        if (containerName.isPresent()) {
            return dataManagementService.downloadData(container, containerName.get(), externalObjectDirectoryEntity.getExternalLocation());
        }
        throw new FileNotDownloadedException(externalObjectDirectoryEntity.getExternalLocation(), container.name(), "Container not found.");
    }


    public String getContainerNameRequired(DatastoreContainerType datastoreContainerType) {
        return getContainerName(datastoreContainerType)
            .orElseThrow(() -> new IllegalArgumentException("Container name cannot be resolved"));
    }

    @Override
    public Optional<String> getContainerName(DatastoreContainerType datastoreContainerType) {
        switch (datastoreContainerType) {
            case INBOUND -> {
                return Optional.of(getInboundContainerName());
            }
            case OUTBOUND -> {
                return Optional.of(getOutboundContainerName());
            }
            case UNSTRUCTURED -> {
                return Optional.of(getUnstructuredContainerName());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public StorageConfiguration getConfiguration() {
        return dataManagementConfiguration;
    }
}