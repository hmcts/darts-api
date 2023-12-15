package uk.gov.hmcts.darts.arm.dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

public interface ArmDataManagementDao {

    BlobContainerClient getBlobContainerClient(String containerName);

    BlobClient getBlobClient(BlobContainerClient containerClient, String blobName);
}
