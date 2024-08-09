package uk.gov.hmcts.darts.stub;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Component
public class DataManagementService implements uk.gov.hmcts.darts.datamanagement.service.DataManagementService {
    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {
        return null;
    }

    @Override
    public Path downloadBlobToFile(String containerName, UUID blobId, String inboundWorkspace) {
        return null;
    }

    @Override
    public UUID saveBlobData(String containerName, BinaryData binaryData) {
        return null;
    }

    @Override
    public BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream, Map<String, String> metadata) {
        return null;
    }

    @Override
    public BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream) {
        return null;
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        return null;
    }

    @Override
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public void copyBlobData(String sourceContainerName, String destinationContainerName, String sourceLocation, String destinationLocation) {

    }

    @Override
    public void addMetaData(BlobClient client, Map<String, String> metadata) {

    }

    @Override
    public Response<Boolean> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException {
        return null;
    }

    @Override
    public DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, UUID blobId) throws FileNotDownloadedException {
        return null;
    }
}