package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("intTest")
public class ArmServiceStubImpl implements ArmService {
    @Override
    public String saveBlobData(String containerName, String filename, BinaryData binaryData) {
        logStubUsageWarning();
        log.warn("Returning filename to mimic successful upload: {}", filename);
        return filename;
    }

    @Override
    public Map<String, BlobItem> listCollectedBlobs(String containerName, String filename) {
        return null;
    }

    @Override
    public Map<String, BlobItem> listResponseBlobs(String containerName, String filename) {
        return null;
    }


    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }
}
