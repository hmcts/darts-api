package uk.gov.hmcts.darts.datamanagement.dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import java.util.UUID;

public interface DataManagementDao {

    BlobServiceClient getBlobClient();

    BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobName);

    BlobContainerClient getBlobContainerClient(String containerName);


}
