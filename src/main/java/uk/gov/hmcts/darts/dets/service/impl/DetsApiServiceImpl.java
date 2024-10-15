package uk.gov.hmcts.darts.dets.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetsApiServiceImpl implements DetsApiService {

    private final DataManagementAzureClientFactory blobServiceFactory;

    private final DetsDataManagementConfiguration configuration;

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final AzureCopyUtil azureCopyUtil;

    @Override
    @SuppressWarnings({"PMD.CloseResource"})
    public DownloadResponseMetaData downloadData(UUID blobId) throws FileNotDownloadedException {
        DownloadResponseMetaData downloadResponseMetaData = new FileBasedDownloadResponseMetaData();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClientWithSasEndpoint(configuration.getSasEndpoint());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(configuration.getContainerName(), serviceClient);

        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        boolean exists = blobClient.exists() != null && blobClient.exists();

        if (!exists) {
            log.error("Blob {} does not exist in {} container", blobId, configuration.getContainerName());
        }

        try (OutputStream downloadOS = downloadResponseMetaData.getOutputStream(configuration)) {
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

    @Override
    public boolean deleteBlobDataFromContainer(UUID blobId) throws AzureDeleteBlobException {
        try {
            BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClientWithSasEndpoint(configuration.getSasEndpoint());
            BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(configuration.getContainerName(), serviceClient);
            BlobClient client = blobServiceFactory.getBlobClient(containerClient, blobId);
            Response<Boolean> response = client.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null,
                                                                           Duration.of(
                                                                               configuration.getDeleteTimeout(),
                                                                               ChronoUnit.SECONDS
                                                                           ), null
            );

            HttpStatus httpStatus = valueOf(response.getStatusCode());
            if (httpStatus.is2xxSuccessful() || NOT_FOUND.equals(httpStatus)) {
                return response.getValue();
            } else {
                String message = String.format("Failed to delete from storage container=%s, blobId=%s, httpStatus=%s",
                                               configuration.getContainerName(), blobId, httpStatus);
                throw new AzureDeleteBlobException(message);
            }

        } catch (RuntimeException e) {
            throw new AzureDeleteBlobException(
                "Could not delete from storage container=" + configuration.getContainerName() + ", blobId=" + blobId, e
            );
        }
    }

    @Override
    public void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName) {
        try {
            String sourceContainerSasUrl = configuration.getSasEndpoint();
            String destinationContainerSasUrl = armDataManagementConfiguration.getSasEndpoint();
            String sourceBlobSasUrl = buildBlobSasUrl(configuration.getContainerName(), sourceContainerSasUrl, detsUuid);
            String destinationBlobSasUrl = buildBlobSasUrl(armDataManagementConfiguration.getContainerName(), destinationContainerSasUrl, blobPathAndName);

            azureCopyUtil.copy(sourceBlobSasUrl, destinationBlobSasUrl);

            log.info("Copy completed from '{}' to '{}'. Source location: {}, destination location: {}",
                     configuration.getContainerName(), armDataManagementConfiguration.getContainerName(), detsUuid, blobPathAndName);
        } catch (Exception e) {
            throw new DartsException(String.format("Exception copying file from '%s' to '%s'. Source location: %s",
                                                   configuration.getContainerName(), armDataManagementConfiguration.getContainerName(), detsUuid), e);
        }
    }

    private String buildBlobSasUrl(String containerName, String containerSasUrl, String location) {
        if (containerName.equals(armDataManagementConfiguration.getSasEndpoint())) {
            // arm sas url contains folder 'DARTS' in the url, so replacing it to avoid 'DARTS' being present twice in the generated blob sas url
            return containerSasUrl.replace(containerName + "/DARTS", containerName + "/" + location);
        } else {
            return containerSasUrl.replace(containerName, containerName + "/" + location);
        }
    }

}