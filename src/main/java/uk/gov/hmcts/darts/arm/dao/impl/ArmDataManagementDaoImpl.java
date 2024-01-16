package uk.gov.hmcts.darts.arm.dao.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArmDataManagementDaoImpl implements ArmDataManagementDao {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private BlobServiceClient blobServiceClient;

    @Override
    public BlobContainerClient getBlobContainerClient(String containerName) {
        if (blobServiceClient == null) {
            blobServiceClient = getBlobServiceClient();
        }
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public BlobClient getBlobClient(BlobContainerClient containerClient, String blobName) {
        return containerClient.getBlobClient(blobName);
    }

    private BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder()
            .endpoint(armDataManagementConfiguration.getSasEndpoint())
            .buildClient();
    }
}
