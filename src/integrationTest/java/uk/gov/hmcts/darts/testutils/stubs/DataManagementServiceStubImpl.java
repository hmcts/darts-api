package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * This class is a test implementation of DataManagementService, intended to mimic the basic behaviour of Azure
 * Blob Storage. TODO: Hopefully this will be replaced by a more functional implementation (see DMP-597).
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Profile("intTest")
public class DataManagementServiceStubImpl implements DataManagementService {

    private final DataManagementConfiguration dataManagementConfiguration;

    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {
        logStubUsageWarning();

        log.warn("Returning dummy file to mimic Blob storage download");
        return BinaryData.fromBytes(new byte[1024]);
    }

    @SneakyThrows
    @Override
    public Path downloadBlobToFile(String containerName, UUID blobId, String inboundWorkspace) {
        logStubUsageWarning();

        log.warn("Downloading blob to dummy file to mimic Blob storage download to file");
        File downloadedBlobDataFile = File.createTempFile("dataManagementServiceStubBlobFile", ".tmp");
        Files.write(downloadedBlobDataFile.toPath(), "someContent".getBytes());
        return downloadedBlobDataFile.toPath();
    }

    @Override
    public UUID saveBlobData(String containerName, BinaryData binaryData) {
        return saveBlobData();
    }

    @Override
    public UUID saveBlobData(String containerName, InputStream inputStream) {
        return saveBlobData();
    }

    private UUID saveBlobData() {
        logStubUsageWarning();

        UUID uuid = UUID.randomUUID();
        log.warn("Returning random UUID to mimic successful upload: {}", uuid);
        return uuid;
    }

    @Override
    public BlobClient saveBlobData(String containerName, BinaryData binaryData, Map<String, String> metadata) {
        logStubUsageWarning();

        UUID uuid = UUID.randomUUID();
        BlobClientBuilder blobClientBuilder = new BlobClientBuilder();
        blobClientBuilder.blobName(uuid.toString());
        blobClientBuilder.endpoint("http://127.0.0.1:10000/devstoreaccount1");
        blobClientBuilder.containerName(containerName);
        log.warn("Returning random UUID to mimic successful upload: {}", uuid);
        return blobClientBuilder.buildClient();
    }

    @Override
    public void copyBlobData(String sourceContainer, String destination, UUID sourceBlobId) {
        logStubUsageWarning();

        log.warn("Dummy method in place for blob copy from two storage locations");
    }

    @Override
    public void addMetaData(BlobClient client, Map<String, String> metadata) {

    }

    @Override
    public Response<Boolean> deleteBlobData(String containerName, UUID blobId) {
        logStubUsageWarning();

        log.info("Delete blob data method executed");
        return null;
    }

    @Override
    @SneakyThrows
    public DownloadResponseMetaData downloadData(DatastoreContainerType type, String containerName, UUID blobId) throws FileNotDownloadedException {
        logStubUsageWarning();

        FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData();


        try (OutputStream downloadOS = fileBasedDownloadResponseMetaData.getOutputStream(dataManagementConfiguration)) {
            Date downloadStartDate = new Date();
            downloadOS.write(new byte[1024]);
            Date downloadEndDate = new Date();
            log.debug("**Downloading of guid {}, took {}ms", blobId, downloadEndDate.getTime() - downloadStartDate.getTime());
        }
        return fileBasedDownloadResponseMetaData;
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }
}