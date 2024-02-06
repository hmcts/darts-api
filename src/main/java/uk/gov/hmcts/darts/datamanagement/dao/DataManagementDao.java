package uk.gov.hmcts.darts.datamanagement.dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import java.util.UUID;

public interface DataManagementDao {

    BlobContainerClient getBlobContainerClient(String containerName);

    BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobName);
}
