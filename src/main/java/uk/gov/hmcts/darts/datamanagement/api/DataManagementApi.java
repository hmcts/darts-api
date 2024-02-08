package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.Map;
import java.util.UUID;

public interface DataManagementApi extends BlobContainerDownloadable {

    BinaryData getBlobDataFromUnstructuredContainer(UUID blobId);

    BinaryData getBlobDataFromOutboundContainer(UUID blobId);

    BinaryData getBlobDataFromInboundContainer(UUID blobId);

    UUID saveBlobDataToOutboundContainer(BinaryData binaryData);

    BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata);

    void addMetadata(BlobClient client, Map<String, String> metadata);

    void addMetadata(BlobClient client, String key, String value);

    void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureDeleteBlobException;

    UUID saveBlobDataToInboundContainer(BinaryData binaryData);

}