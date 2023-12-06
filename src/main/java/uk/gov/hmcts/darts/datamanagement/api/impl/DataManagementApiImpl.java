package uk.gov.hmcts.darts.datamanagement.api.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
    public UUID saveBlobDataToOutboundContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(getOutboundContainerName(), binaryData);
    }

    @Override
    public BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata) {
        String containerName = getContainerName(container);
        return dataManagementService.saveBlobData(containerName, binaryData, metadata);
    }

    @Override
    public void addMetadata(BlobClient client, Map<String, String> metadata) {
        dataManagementService.addMetaData(client, metadata);
    }

    @Override
    public void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getOutboundContainerName(), blobId);
    }

    @Override
    public void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getOutboundContainerName(), blobId);
    }

    @Override
    public void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureDeleteBlobException {
        dataManagementService.deleteBlobData(getUnstructuredContainerName(), blobId);
    }


    @Override
    public UUID saveBlobDataToInboundContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(getInboundContainerName(), binaryData);
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

    private String getContainerName(DatastoreContainerType datastoreContainerType) {
        switch (datastoreContainerType) {
            case INBOUND -> {
                return getInboundContainerName();
            }
            case OUTBOUND -> {
                return getOutboundContainerName();
            }
            case UNSTRUCTURED -> {
                return getUnstructuredContainerName();
            }
            default -> {
                return null;
            }
        }
    }

}
