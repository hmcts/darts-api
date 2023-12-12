package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.model.ArmBlobInfo;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("intTest")
public class ArmServiceStubImpl implements ArmService {
    @Override
    public ArmBlobInfo saveBlobData(String containerName, String filename, BinaryData binaryData) {
        logStubUsageWarning();

        String blobName = UUID.randomUUID().toString();
        log.warn("Returning filename to mimic successful upload: {}", blobName);
        return ArmBlobInfo.builder()
            .blobPathAndName(filename)
            .blobName(blobName)
            .build();
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");
    }
}
