package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({
    "checkstyle:SummaryJavadoc",
    "PMD.CouplingBetweenObjects",
    "PMD.TooManyMethods"
})
public class DataManagementServiceImpl implements DataManagementService {

    public static final String BLOB_DOES_NOT_EXIST_IN_CONTAINER = "Blob {} does not exist in {} container";
    private final DataManagementConfiguration dataManagementConfiguration;

    private final DataManagementAzureClientFactory blobServiceFactory;

    private final AzureCopyUtil azureCopyUtil;
    private final FileContentChecksum fileContentChecksum;

    /**
     * Note: This implementation is not memory-efficient with large files
     * use an implementation that stores the blob to a temp file instead.
     */
    @Override
    public BinaryData getBlobData(String containerName, String blobId) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        if (!blobClient.exists()) {
            log.error(BLOB_DOES_NOT_EXIST_IN_CONTAINER, blobId, containerName);
        }

        LocalDateTime downloadStartDate = LocalDateTime.now();
        BinaryData binaryData = blobClient.downloadContent();
        LocalDateTime downloadEndDate = LocalDateTime.now();
        Duration downloadDuration = Duration.between(downloadStartDate, downloadEndDate);

        log.debug("Downloading of blob data guid {}, took {}ms", blobId, downloadDuration.toMillis());

        return binaryData;
    }

    @Override
    @SneakyThrows
    public Path downloadBlobToFile(String containerName, String blobId, String workspace) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        if (!blobClient.exists()) {
            log.error(BLOB_DOES_NOT_EXIST_IN_CONTAINER, blobId, containerName);
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
    public String saveBlobData(String containerName, BinaryData binaryData) {

        String uniqueBlobId = UUID.randomUUID().toString();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        BlobClient client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);

        return uniqueBlobId;
    }

    @Override
    public BlobClientUploadResponseImpl saveBlobData(String containerName, InputStream inputStream, Map<String, String> metadata) {
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        var uniqueBlobId = UUID.randomUUID().toString();

        var client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);

        var uploadOptions = new BlobParallelUploadOptions(inputStream);
        uploadOptions.setMetadata(metadata);
        uploadOptions.setParallelTransferOptions(createCommonTransferOptions());
        client.uploadWithResponse(uploadOptions, dataManagementConfiguration.getBlobClientTimeout(), null);

        return new BlobClientUploadResponseImpl(client);
    }

    @Override
    public BlobClientUploadResponseImpl saveBlobData(String containerName, InputStream inputStream) {
        return saveBlobData(containerName, inputStream, null);
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        String uniqueBlobId = UUID.randomUUID().toString();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);

        BlobClient client = blobServiceFactory.getBlobClient(containerClient, uniqueBlobId);
        client.upload(binaryData);
        client.setMetadata(metadata);
        return client;
    }

    @SneakyThrows
    @Override
    @SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.AvoidInstanceofChecksInCatchClause", "java:S1193"})
    public void copyBlobData(String sourceContainerName, String destinationContainerName, String sourceLocation, String destinationLocation) {
        try {
            String sourceContainerSasUrl = dataManagementConfiguration.getContainerSasUrl(sourceContainerName);
            String destinationContainerSasUrl = dataManagementConfiguration.getContainerSasUrl(destinationContainerName);
            String sourceBlobSasUrl = buildBlobSasUrl(sourceContainerName, sourceContainerSasUrl, sourceLocation);
            String destinationBlobSasUrl = buildBlobSasUrl(destinationContainerName, destinationContainerSasUrl, destinationLocation);

            azureCopyUtil.copy(sourceBlobSasUrl, destinationBlobSasUrl);

            log.info("Copy completed from '{}' to '{}'. Source location: {}, destination location: {}",
                     sourceContainerName, destinationContainerName, sourceLocation, destinationLocation);

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                log.error("Exception copying file from '{}' to '{}'. Source location: {}, destination location: {}",
                          sourceContainerName, destinationContainerName, sourceLocation, destinationLocation, e);
                throw e;
            }
            throw new DartsException(String.format("Exception copying file from '%s' to '%s'. Source location: %s",
                                                   sourceContainerName, destinationContainerName, sourceLocation), e);
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
    public DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, String blobId) throws FileNotDownloadedException {
        DownloadResponseMetaData downloadResponse = new FileBasedDownloadResponseMetaData();
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);
        boolean exists = blobClient.exists() != null && blobClient.exists();

        if (!exists) {
            log.error(BLOB_DOES_NOT_EXIST_IN_CONTAINER, blobId, containerName);
            throw new FileNotDownloadedException(blobId, containerName, "Blob doesn't exist in container.");
        }

        try (OutputStream downloadOS = downloadResponse.getOutputStream(dataManagementConfiguration)) {
            LocalDateTime downloadStartDate = LocalDateTime.now();
            blobClient.downloadStream(downloadOS);

            LocalDateTime downloadEndDate = LocalDateTime.now();
            Duration downloadDuration = Duration.between(downloadStartDate, downloadEndDate);
            log.debug("Downloading of guid {}, took {}ms", blobId, downloadDuration.toMillis());

            downloadResponse.setContainerTypeUsedToDownload(type);
        } catch (IOException e) {
            log.error("Error trying to download Blob {} from container {}", blobId, containerName, e);
            throw new FileNotDownloadedException(blobId, containerName, "Error trying to download blob", e);
        }
        return downloadResponse;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")//TODO - refactor to avoid deeply nested if statements when this class is next edited
    public String getChecksum(String containerName, String blobId) {
        log.info("Getting checksum for blob '{}' in container '{}'", blobId, containerName);
        BlobServiceClient serviceClient = blobServiceFactory.getBlobServiceClient(dataManagementConfiguration.getBlobStorageAccountConnectionString());
        BlobContainerClient containerClient = blobServiceFactory.getBlobContainerClient(containerName, serviceClient);
        BlobClient blobClient = blobServiceFactory.getBlobClient(containerClient, blobId);

        boolean exists = blobClient.exists() != null && blobClient.exists();

        if (!exists) {
            throw new DartsApiException(CommonApiError.NOT_FOUND,
                                        String.format("Blob '%s' does not exist in container '%s'.", blobId, containerName));
        }
        BlobProperties blobProperties = blobClient.getProperties();
        byte[] checksumByte = blobProperties.getContentMd5();

        if (checksumByte == null || checksumByte.length == 0) {
            Map<String, String> metaData = blobProperties.getMetadata();
            if (metaData.containsKey("checksum")) {
                String checksum = metaData.get("checksum");
                if (!StringUtils.isBlank(checksum)) {
                    return checksum;
                }
            }
            throw new DartsApiException(CommonApiError.NOT_FOUND,
                                        String.format("Blob '%s' does exist in container '%s' but does not contain a checksum.", blobId, containerName));
        }
        log.info("Finished getting checksum for blob '{}' in container '{}'", blobId, containerName);
        return fileContentChecksum.encodeToString(checksumByte);
    }


    @Override
    public Response<Boolean> deleteBlobData(String containerName, String blobId) throws AzureDeleteBlobException {
        log.info("About to delete blob id {} from container {}", blobId, containerName);
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

    private String buildBlobSasUrl(String containerName, String containerSasUrl, String location) {
        if (containerName.equals(dataManagementConfiguration.getArmContainerName())) {
            // arm sas url contains folder 'DARTS' in the url, so replacing it to avoid 'DARTS' being present twice in the generated blob sas url
            return containerSasUrl.replace(containerName + "/DARTS", containerName + "/" + location);
        } else {
            return containerSasUrl.replace(containerName, containerName + "/" + location);
        }
    }

}