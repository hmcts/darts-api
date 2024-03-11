package uk.gov.hmcts.darts.dets.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.DetsApiService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetsApiServiceImpl implements DetsApiService {

    private final DataManagementAzureClientFactory blobServiceFactory;

    private final DetsDataManagementConfiguration configuration;

    @Override
    public DownloadResponseMetaData downloadData(UUID blobId) throws FileNotDownloadedException {
        DownloadResponseMetaData downloadResponseMetaData = new FileBasedDownloadResponseMetaData();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClientWithSasEndpoint(configuration.getSasEndpoint());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(configuration.getContainerName(), serviceClient);

        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        boolean exists = blobClient.exists() != null && blobClient.exists();

        if (!exists) {
            log.error("Blob {} does not exist in {} container", blobId, configuration.getContainerName());
        }

        try {
            OutputStream downloadOS = downloadResponseMetaData.getOutputStream(configuration);
            Date downloadStartDate = new Date();
            blobClient.downloadStream(downloadOS);
            Date downloadEndDate = new Date();
            downloadResponseMetaData.setContainerTypeUsedToDownload(DatastoreContainerType.DETS);
            log.debug("**Downloading of guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());
            return downloadResponseMetaData;
        } catch (IOException e) {
            throw new FileNotDownloadedException(blobId, configuration.getContainerName(), "File not downloaded from DETS", e);
        }
    }

    @Override
    public UUID saveBlobData(BinaryData binaryData) {
        UUID uniqueBlobId = UUID.randomUUID();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClientWithSasEndpoint(configuration.getSasEndpoint());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(configuration.getContainerName(), serviceClient);

        BlobClient client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);
        return uniqueBlobId;
    }
}