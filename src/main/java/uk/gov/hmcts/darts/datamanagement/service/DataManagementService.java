package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.UUID;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, UUID blobId);

    UUID saveBlobData(String containerName, BinaryData binaryData);

    Response<Void> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException;
}
