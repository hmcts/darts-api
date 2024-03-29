package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.ArrayList;
import java.util.List;

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
    public String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName) {
        logStubUsageWarning();
        log.warn("Returning filename to mimic successful upload: {}", blobPathAndName);
        return blobPathAndName;
    }

    @Override
    public List<String> listSubmissionBlobs(String containerName, String filename) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public List<String> listCollectedBlobs(String containerName, String filename) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public List<String> listResponseBlobs(String containerName, String filename) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public BinaryData getBlobData(String containerName, String blobName) {
        return BinaryData.fromBytes(new byte[1024]);
    }

    @Override
    public boolean deleteBlobData(String containerName, String blobPathAndName) {
        logStubUsageWarning();
        return true;
    }

    private void logStubUsageWarning() {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                 + " you should ask questions! ###");
    }
}
