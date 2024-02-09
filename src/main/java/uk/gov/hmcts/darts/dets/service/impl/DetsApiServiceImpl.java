package uk.gov.hmcts.darts.dets.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.ResponseMetaData;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.DetsApiService;

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
    @SneakyThrows
    public void downloadData(UUID blobId, ResponseMetaData report) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(configuration.getConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(configuration.getContainerName(), serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);

        if (!blobClient.exists()) {
            log.error("Blob {} does not exist in {} container", blobId, configuration.getContainerName());
        }

        try (OutputStream downloadOS = report.getOutputStream()) {
            Date downloadStartDate = new Date();
            blobClient.downloadStream(downloadOS);
            Date downloadEndDate = new Date();

            log.debug("**Downloading of guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());

            report.markProcessed();
            report.markSuccess(null);

        }
    }
}