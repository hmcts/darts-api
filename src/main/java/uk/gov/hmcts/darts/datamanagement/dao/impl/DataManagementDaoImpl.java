package uk.gov.hmcts.darts.datamanagement.dao.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataManagementDaoImpl implements DataManagementDao {

    private final DataManagementConfiguration dataManagementConfiguration;
    private BlobServiceClient blobServiceClient;

    @Override
    public BlobContainerClient getBlobContainerClient(String containerName) {
        if (blobServiceClient == null) {
            blobServiceClient = getBlobServiceClient();
        }
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobId) {
        return containerClient.getBlobClient(String.valueOf(blobId));
    }

    private BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .buildClient();
    }
}
