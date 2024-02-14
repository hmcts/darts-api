package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

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

    @Override
    public BinaryData getBlobData(String containerName, UUID blobId) {
        logStubUsageWarning();

        log.warn("Returning dummy file to mimic Blob storage download");
        return BinaryData.fromBytes(new byte[1024]);
    }

    @Override
    public UUID saveBlobData(String containerName, BinaryData binaryData) {
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
    public void addMetaData(BlobClient client, Map<String, String> metadata) {

    }

    @Override
    public Response<Void> deleteBlobData(String containerName, UUID blobId) {
        log.info("Delete blob data method executed");
        return null;
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }

    @Override
    public void downloadData(DatastoreContainerType type, String containerName, UUID blobId, DownloadResponseMetaData report) {

    }
}