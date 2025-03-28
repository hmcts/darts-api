package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, String blobId);

    Path downloadBlobToFile(String containerName, String blobId, String inboundWorkspace);

    String saveBlobData(String containerName, BinaryData binaryData);

    BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream, Map<String, String> metadata);

    BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream);

    BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata);

    void copyBlobData(String sourceContainerName, String destinationContainerName, String sourceLocation, String destinationLocation);

    void addMetaData(BlobClient client, Map<String, String> metadata);

    String getChecksum(String containerName, String blobId);

    Response<Boolean> deleteBlobData(String containerName, String blobId) throws AzureDeleteBlobException;

    DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, String blobId) throws FileNotDownloadedException;
}