package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.datamanagement.component.MediaDownloadMetaData;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.Map;
import java.util.UUID;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, UUID blobId);

    UUID saveBlobData(String containerName, BinaryData binaryData);

    BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata);

    void addMetaData(BlobClient client, Map<String, String> metadata);

    Response<Void> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException;

    boolean downloadData(String containerName, UUID blobId, MediaDownloadMetaData report);
}