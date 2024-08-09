package uk.gov.hmcts.darts.stub;

import com.azure.core.util.BinaryData;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArmService implements uk.gov.hmcts.darts.arm.service.ArmService {
    @Override
    public String saveBlobData(String containerName, String filename, BinaryData binaryData) {
        return null;
    }

    @Override
    public String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName) {
        return null;
    }

    @Override
    public List<String> listSubmissionBlobs(String containerName, String filename) {
        return new ArrayList<>();
    }

    @Override
    public List<String> listResponseBlobs(String containerName, String filename) {
        return new ArrayList<>();
    }

    @Override
    public List<String> listSubmissionBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        return new ArrayList<>();
    }

    @Override
    public List<String> listResponseBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        return new ArrayList<>();
    }

    @Override
    public ContinuationTokenBlobs listResponseBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken) {
        return null;
    }

    @Override
    public ContinuationTokenBlobs listSubmissionBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken) {
        return null;
    }

    @Override
    public BinaryData getBlobData(String containerName, String blobPathAndName) {
        return null;
    }

    @Override
    public boolean deleteBlobData(String containerName, String blobPathAndName) {
        return false;
    }
}