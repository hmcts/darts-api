package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.test.common.TestUtils.getFile;

/**
 * This class is a test implementation of DataManagementService, intended to mimic the basic behaviour of Azure
 * Blob Storage. TODO: Hopefully this will be replaced by a more functional implementation (see DMP-597).
 */
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class DataManagementServiceStubImpl implements DataManagementService {

    // use this UUID to request stub throwing an exception
    public static final String FAILURE_UUID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private final DataManagementConfiguration dataManagementConfiguration;

    @Value("${darts.audio.transformation.service.audio.file:#{null}}")
    private String audioFile;

    @Override
    public BinaryData getBlobData(String containerName, String blobId) {
        logStubUsageWarning();

        log.warn("Returning dummy file to mimic Blob storage download");
        return BinaryData.fromBytes(new byte[1024]);
    }

    @SneakyThrows
    @Override
    public Path downloadBlobToFile(String containerName, String blobId, String inboundWorkspace) {
        logStubUsageWarning();

        log.warn("Downloading blob to dummy file to mimic Blob storage download to file");
        File downloadedBlobDataFile = File.createTempFile("dataManagementServiceStubBlobFile", ".tmp");
        Files.write(downloadedBlobDataFile.toPath(), "someContent".getBytes());
        return downloadedBlobDataFile.toPath();
    }

    @Override
    public String saveBlobData(String containerName, BinaryData binaryData) {
        return saveBlobData();
    }

    @Override
    public BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream, Map<String, String> metadata) {
        return saveBlobData(containerName, inputStream);
    }

    @Override
    public BlobClientUploadResponse saveBlobData(String containerName, InputStream inputStream) {
        String blobName = saveBlobData();
        long blobSize = 1000L; // Some arbitrary value

        return new BlobClientUploadResponseStub(blobName, blobSize);
    }

    private String saveBlobData() {
        logStubUsageWarning();

        String uuid = UUID.randomUUID().toString();
        log.warn("Returning random UUID to mimic successful upload: {}", uuid);
        return uuid;
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        logStubUsageWarning();

        String uuid = UUID.randomUUID().toString();
        BlobClientBuilder blobClientBuilder = new BlobClientBuilder();
        blobClientBuilder.blobName(uuid);
        blobClientBuilder.endpoint("http://127.0.0.1:10000/devstoreaccount1");
        blobClientBuilder.containerName(containerName);
        log.warn("Returning random UUID to mimic successful upload: {}", uuid);
        return blobClientBuilder.buildClient();
    }

    @Override
    public void copyBlobData(String sourceContainerName, String destinationContainerName, String sourceLocation, String destinationLocation) {
        logStubUsageWarning();

        if (FAILURE_UUID.equals(sourceLocation)) {
            throw new DartsException("Exception thrown since copy requested with failure UUID");
        }

        log.debug("Copy blob from '{}' to '{}' executed. Source location '{}, destination location '{}'",
                  sourceContainerName, destinationContainerName, sourceLocation, destinationLocation);
    }

    @Override
    public void addMetaData(BlobClient client, Map<String, String> metadata) {

    }

    @Override
    public String getChecksum(String containerName, String blobId) {
        return "checksum-" + blobId;
    }

    @Override
    public Response<Boolean> deleteBlobData(String containerName, String blobId) {
        logStubUsageWarning();

        log.info("Delete blob data method executed");
        return null;
    }

    @Override
    @SneakyThrows
    public DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, String blobId) throws FileNotDownloadedException {
        logStubUsageWarning();

        FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData();

        byte[] audio = new byte[1024];
        if (nonNull(audioFile)) {
            File audioFileTest = getFile(audioFile);
            if (nonNull(audioFileTest) && audioFileTest.exists()) {
                audio = Files.newInputStream(audioFileTest.toPath()).readAllBytes();
            } else {
                log.warn("Unable to read audio file {}", audioFile);
            }
        }
        try (OutputStream downloadOS = fileBasedDownloadResponseMetaData.getOutputStream(dataManagementConfiguration)) {
            LocalDateTime downloadStartDate = LocalDateTime.now();
            downloadOS.write(audio);
            LocalDateTime downloadEndDate = LocalDateTime.now();
            Duration downloadDuration = Duration.between(downloadStartDate, downloadEndDate);
            log.debug("Downloading of guid {}, took {}ms", blobId, downloadDuration.toMillis());
        }
        return fileBasedDownloadResponseMetaData;
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }

    @Getter
    @RequiredArgsConstructor
    public static class BlobClientUploadResponseStub implements BlobClientUploadResponse {

        private final String blobName;
        private final Long blobSize;

        @Override
        public Map<String, String> addMetadata(Map<String, String> additionalMetadata) {
            return additionalMetadata;
        }

    }

}