package uk.gov.hmcts.darts.testutils.stubs;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Deprecated
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
    public List<String> listResponseBlobs(String containerName, String filename) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public List<String> listSubmissionBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public List<String> listResponseBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        List<String> listedBlobs = new ArrayList<>();
        listedBlobs.add(filename);
        return listedBlobs;
    }

    @Override
    public ContinuationTokenBlobs listResponseBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken) {
        return ContinuationTokenBlobs.builder().build();
    }

    @Override
    public ContinuationTokenBlobs listSubmissionBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken) {
        return ContinuationTokenBlobs.builder().build();
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