package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, UUID blobId);

    Path downloadBlobToFile(String containerName, UUID blobId, String inboundWorkspace);

    UUID saveBlobData(String containerName, BinaryData binaryData);

    UUID saveBlobData(String containerName, InputStream inputStream);

    BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata);

    void copyBlobData(String sourceContainerName, String destinationContainerName, UUID sourceBlobId);

    void addMetaData(BlobClient client, Map<String, String> metadata);

    Response<Boolean> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException;

    DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, UUID blobId) throws FileNotDownloadedException;
}