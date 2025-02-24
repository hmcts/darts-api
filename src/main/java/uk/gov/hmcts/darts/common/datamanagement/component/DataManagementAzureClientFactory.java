package uk.gov.hmcts.darts.common.datamanagement.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

public interface DataManagementAzureClientFactory {
    BlobContainerClient getBlobContainerClient(String containerName,  BlobServiceClient serviceClient);

    BlobClient getBlobClient(BlobContainerClient containerClient, String blobName);

    BlobServiceClient getBlobServiceClient(String containerString);

    BlobServiceClient getBlobServiceClientWithSasEndpoint(String sasEndpoint);
}