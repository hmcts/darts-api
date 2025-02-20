package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

public interface DataManagementApi extends BlobContainerDownloadable {

    DownloadResponseMetaData getBlobDataFromOutboundContainer(UUID blobId) throws FileNotDownloadedException;

    BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata);

    BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container, Map<String, String> metadata);

    BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container);

    void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureDeleteBlobException;

    UUID saveBlobDataToInboundContainer(BinaryData binaryData);

    UUID saveBlobDataToUnstructuredContainer(BinaryData binaryData);

    String getChecksum(DatastoreContainerType datastoreContainerType, UUID guid);
}
