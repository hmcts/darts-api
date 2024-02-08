package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataManagementAzureClientFactoryImpl implements DataManagementAzureClientFactory {

    private final DataManagementConfiguration dataManagementConfiguration;
    private Map<String, BlobContainerClient> blobServiceClientMap =  new HashMap<>();

    private BlobServiceClient blobServiceClient;

    @Override
    public BlobContainerClient getBlobContainerClient(String containerName, BlobServiceClient serviceClient) {
        String id = serviceClient.getAccountUrl() + ":" + containerName;
        if (!blobServiceClientMap.containsKey(id)) {
            blobServiceClientMap.put(id, serviceClient.getBlobContainerClient(containerName));
        }
        return blobServiceClientMap.get(id);
    }

    @Override
    public BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobId) {
        return containerClient.getBlobClient(String.valueOf(blobId));
    }

    public BlobServiceClient getBlobServiceClient(String containerString) {
        if (blobServiceClient == null) {
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(containerString)
                    .buildClient();
        }

        return blobServiceClient;
    }


}