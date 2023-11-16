package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.UUID;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class DataManagementServiceImpl implements DataManagementService {

    private final DataManagementDao dataManagementDao;


    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {

        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
        BlobClient blobClient = dataManagementDao.getBlobClient(containerClient, blobId);
        return blobClient.downloadContent();
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
    public void deleteBlobData(String containerName, UUID blobId) {

        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);
        BlobClient blobClient = dataManagementDao.getBlobClient(containerClient, blobId);
        blobClient.delete();
    }
}
