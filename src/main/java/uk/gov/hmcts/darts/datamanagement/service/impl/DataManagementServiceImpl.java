package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class DataManagementServiceImpl implements DataManagementService {

    private final DataManagementConfiguration dataManagementConfiguration;

    private final DataManagementAzureClientFactory blobServiceFactory;

    private final AzureCopyUtil azureCopyUtil;


    /**
     * Note: This implementation is not memory-efficient with large files,
     * use downloadBlobToFile(String containerName, UUID blobId, String workspace) instead.
     */
    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        if (!blobClient.exists()) {
            log.error("Blob {} does not exist in {} container", blobId, containerName);
        }

        Date downloadStartDate = new Date();
        BinaryData binaryData = blobClient.downloadContent();
        Date downloadEndDate = new Date();
        log.debug("**Downloading of guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());

        return binaryData;
    }

    @Override
    @SneakyThrows
    public Path downloadBlobToFile(String containerName, UUID blobId, String workspace) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        if (!blobClient.exists()) {
            log.error("Blob {} does not exist in {} container", blobId, containerName);
        }

        Path workspacePath = Path.of(workspace);
        Path targetFile = workspacePath.resolve(UUID.randomUUID() + ".tmp");
        Files.createDirectories(workspacePath);

        log.debug("started downloading blob {} to {}", blobId, targetFile.toAbsolutePath());
        blobClient.downloadToFile(targetFile.toString());
        log.debug("finished downloading blob {}", blobId);
        return targetFile;
    }

    /**
     * @deprecated This implementation is not memory-efficient with large files, use saveBlobData(String containerName, InputStream inputStream) instead.
     */
    @Deprecated
    @Override
    public UUID saveBlobData(String containerName, BinaryData binaryData) {

        UUID uniqueBlobId = UUID.randomUUID();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        BlobClient client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);

        return uniqueBlobId;
    }

    @Override
    public UUID saveBlobData(String containerName, InputStream inputStream) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        var uniqueBlobId = UUID.randomUUID();

        var client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);

        var uploadOptions = new BlobParallelUploadOptions(inputStream);
        uploadOptions.setParallelTransferOptions(createCommonTransferOptions());
        client.uploadWithResponse(uploadOptions, dataManagementConfiguration.getBlobClientTimeout(), null);

        return uniqueBlobId;
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        UUID uniqueBlobId = UUID.randomUUID();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        BlobClient client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);
        client.setMetadata(metadata);
        return client;
    }

    @Override
    public void copyBlobData(String sourceContainerName, String destinationContainerName, UUID sourceBlobId) {
        try {
            String sourceContainerSasUrl = dataManagementConfiguration.getContainerSasUrl(sourceContainerName);
            String destinationContainerSasUrl = dataManagementConfiguration.getContainerSasUrl(destinationContainerName);
            String sourceBlobSasUrl = buildBlobSasUrl(sourceContainerName, sourceContainerSasUrl, sourceBlobId.toString());
            String destinationBlobSasUrl = buildBlobSasUrl(destinationContainerName, destinationContainerSasUrl, sourceBlobId.toString());

            azureCopyUtil.copy(sourceBlobSasUrl, destinationBlobSasUrl);

            log.info("Copy completed from '{}' to '{}' for external object directory Id: {}", sourceContainerName, destinationContainerName, sourceBlobId);
        } catch (Exception e) {
            throw new DartsException(String.format("Exception copying file from '%s' to '%s'. External object directory Id: %s",
                    sourceContainerName, destinationContainerName, sourceBlobId), e);
        }
    }

    private ParallelTransferOptions createCommonTransferOptions() {
        return new ParallelTransferOptions()
            .setBlockSizeLong(dataManagementConfiguration.getBlobClientBlockSizeBytes())
            .setMaxSingleUploadSizeLong(dataManagementConfiguration.getBlobClientMaxSingleUploadSizeBytes())
            .setMaxConcurrency(dataManagementConfiguration.getBlobClientMaxConcurrency());
    }

    @Override
    public void addMetaData(BlobClient client, Map<String, String> metadata) {
        Map<String, String> realMetaData = client.getProperties().getMetadata();
        realMetaData.putAll(metadata);
        client.setMetadata(realMetaData);
    }

    @Override
    public DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, UUID blobId) throws FileNotDownloadedException {
        DownloadResponseMetaData downloadResponse = new FileBasedDownloadResponseMetaData();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        boolean exists = blobClient.exists() != null && blobClient.exists();

        if (!exists) {
            log.error("Blob {} does not exist in {} container", blobId, containerName);
            throw new FileNotDownloadedException(blobId, containerName, "Blob doesn't exist in container.");
        }

        try (OutputStream downloadOS = downloadResponse.getOutputStream(dataManagementConfiguration)) {
            Date downloadStartDate = new Date();
            blobClient.downloadStream(downloadOS);

            Date downloadEndDate = new Date();
            log.debug("**Downloading of guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());

            downloadResponse.setContainerTypeUsedToDownload(type);
        } catch (IOException e) {
            log.error("Error trying to download Blob {} from container{}", blobId, containerName, e);
            throw new FileNotDownloadedException(blobId, containerName, "Error trying to download blob", e);
        }
        return downloadResponse;
    }


    @Override
    public Response<Boolean> deleteBlobData(String containerName, UUID blobId) throws AzureDeleteBlobException {
        try {
            BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
            BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
            BlobClient client = blobServiceFactory.getBlobClient(containerClient, blobId);
            Response<Boolean> response = client.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null,
                                                                           Duration.of(
                                                                               dataManagementConfiguration.getDeleteTimeout(),
                                                                               ChronoUnit.SECONDS
                                                                           ), null
            );

            HttpStatus httpStatus = valueOf(response.getStatusCode());
            if (httpStatus.is2xxSuccessful() || NOT_FOUND.equals(httpStatus)) {
                return response;
            } else {
                String message = String.format("Failed to delete from storage container=%s, blobId=%s, httpStatus=%s",
                                               containerName, blobId, httpStatus);
                throw new AzureDeleteBlobException(message);
            }

        } catch (RuntimeException e) {
            throw new AzureDeleteBlobException(
                "Could not delete from storage container=" + containerName + ", blobId=" + blobId, e
            );
        }
    }

    private String buildBlobSasUrl(String containerName, String containerSasUrl, String sourceBlobId) {
        return containerSasUrl.replace(containerName, containerName + "/" + sourceBlobId);
    }
}