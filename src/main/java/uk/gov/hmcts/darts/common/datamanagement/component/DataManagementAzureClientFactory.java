package uk.gov.hmcts.darts.common.datamanagement.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import java.util.UUID;

public interface DataManagementAzureClientFactory {
    BlobContainerClient getBlobContainerClient(String containerName,  BlobServiceClient serviceClient);

    BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobName);

    BlobServiceClient getBlobServiceClient(String containerString);
}