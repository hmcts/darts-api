package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

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
    public void deleteBlobData(String containerName, UUID blobId) {
        log.info("Delete blob data method executed");
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }

}
