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

public interface DataManagementApi extends BlobContainerDownloadable {

    DownloadResponseMetaData getBlobDataFromOutboundContainer(String blobId) throws FileNotDownloadedException;

    BlobClient saveBlobDataToContainer(BinaryData binaryData, DatastoreContainerType container, Map<String, String> metadata);

    BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container, Map<String, String> metadata);

    BlobClientUploadResponse saveBlobToContainer(InputStream inputStream, DatastoreContainerType container);

    void deleteBlobDataFromOutboundContainer(String blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromInboundContainer(String blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromUnstructuredContainer(String blobId) throws AzureDeleteBlobException;

    String saveBlobDataToInboundContainer(BinaryData binaryData);

    String saveBlobDataToInboundContainer(InputStream inputStream);

    String saveBlobDataToUnstructuredContainer(BinaryData binaryData);

    String getChecksum(DatastoreContainerType datastoreContainerType, String guid);
}
