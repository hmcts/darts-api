package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class DataManagementServiceImpl implements DataManagementService {

    private final DataManagementDao dataManagementDao;

    private final DataManagementConfiguration dataManagementConfiguration;

    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {

        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
        BlobClient blobClient = dataManagementDao.getBlobClient(containerClient, blobId);
        if (!blobClient.exists()) {
            log.error("Blob {} does not exist in {} container", blobId, containerName);
        }

        log.debug("Start downloading guid {}", blobId);
        Date downloadStartDate = new Date();
        BinaryData binaryData = blobClient.downloadContent();
        Date downloadEndDate = new Date();
        log.debug("**Finished downloading guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());

        return binaryData;
    }

    @Override
    public UUID saveBlobData(String containerName, BinaryData binaryData) {

        UUID uniqueBlobId = UUID.randomUUID();
        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
        BlobClient client = dataManagementDao.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);

        return uniqueBlobId;
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        UUID uniqueBlobId = UUID.randomUUID();
        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
        BlobClient client = dataManagementDao.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);
        client.setMetadata(metadata);
        return client;
    }

    @Override
    public void addMetaData(BlobClient client, Map<String, String> metadata) {
        Map<String, String> realMetaData = client.getProperties().getMetadata();
        realMetaData.putAll(metadata);
        client.setMetadata(realMetaData);
    }

    @Override
    public Response<Void> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException {
        try {
            BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = dataManagementDao.getBlobClient(containerClient, blobId);
            Response<Void> response = blobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null,
                                                                    Duration.of(
                                                                        dataManagementConfiguration.getDeleteTimeout(),
                                                                        ChronoUnit.SECONDS
                                                                    ), null
            );

            if (202 != response.getStatusCode()) {
                throw new AzureDeleteBlobException("Failed to delete from container because of http code: " + response.getStatusCode());
            }
            return response;
        } catch (RuntimeException e) {
            throw new AzureDeleteBlobException(
                "Could not delete from container: " + containerName + " uuid: " + blobId + " connection-string: " +
                    dataManagementConfiguration.getBlobStorageAccountConnectionString(), e
            );
        }
    }
}
