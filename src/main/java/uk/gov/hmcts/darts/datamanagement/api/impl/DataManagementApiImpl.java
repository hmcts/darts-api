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
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.InputStream;
import java.util.HashMap;
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
    public BinaryData getBlobDataFromUnstructuredContainer(UUID blobId) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), blobId);
    }

    @Override
    public BinaryData getBlobDataFromOutboundContainer(UUID blobId) {
        return dataManagementService.getBlobData(getOutboundContainerName(), blobId);
    }

    @Override
    public BinaryData getBlobDataFromInboundContainer(UUID blobId) {
        return dataManagementService.getBlobData(getInboundContainerName(), blobId);
    }

    @Override
    public BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata) {
        Optional<String> containerName = getContainerName(container);
        return containerName.map(s -> dataManagementService.saveBlobData(s, binaryData, metadata)).orElse(null);

    }

    @Override
    public void addMetadata(BlobClient client, Map<String, String> metadata) {
        dataManagementService.addMetaData(client, metadata);
    }

    @Override
    public void addMetadata(BlobClient client, String key, String value) {
        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put(key, value);
        addMetadata(client, newMetadata);
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
        return dataManagementService.saveBlobData(getInboundContainerName(), inputStream);
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
