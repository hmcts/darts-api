package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataManagementAzureClientFactoryImpl implements DataManagementAzureClientFactory {

    private final Map<String, BlobContainerClient> blobContainerClientMap =  new ConcurrentHashMap<>();
    private final Map<String, BlobServiceClient> blobServiceClientMap =  new ConcurrentHashMap<>();

    @Override
    public BlobContainerClient getBlobContainerClient(String containerName, BlobServiceClient serviceClient) {
        String id = serviceClient.getAccountUrl() + ":" + containerName;
        return blobContainerClientMap.computeIfAbsent(id, k -> serviceClient.getBlobContainerClient(containerName));
    }

    @Override
    public BlobClient getBlobClient(BlobContainerClient containerClient, UUID blobId) {
        return containerClient.getBlobClient(String.valueOf(blobId));
    }

    @Override
    public BlobServiceClient getBlobServiceClient(String containerString) {
        return blobServiceClientMap.computeIfAbsent(containerString, k -> new BlobServiceClientBuilder()
            .connectionString(containerString).buildClient());
    }

    @Override
    public BlobServiceClient getBlobServiceClientWithSasEndpoint(String sasEndpoint) {
        return blobServiceClientMap.computeIfAbsent(sasEndpoint, k -> new BlobServiceClientBuilder()
            .endpoint(sasEndpoint).buildClient());
    }
}