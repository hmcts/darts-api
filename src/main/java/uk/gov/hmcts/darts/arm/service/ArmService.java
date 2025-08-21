package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;

import java.util.List;

public interface ArmService {

    String saveBlobData(String containerName, String filename, BinaryData binaryData);

    String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName);

    List<String> listSubmissionBlobs(String containerName, String filename);

    List<String> listResponseBlobs(String containerName, String filename);

    List<String> listSubmissionBlobsUsingBatch(String containerName, String filename, Integer batchSize);

    List<String> listResponseBlobsUsingBatch(String containerName, String filename, Integer batchSize);

    ContinuationTokenBlobs listResponseBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken);

    ContinuationTokenBlobs listSubmissionBlobsWithMarker(String containerName, String filename, Integer batchSize, String continuationToken);

    BinaryData getBlobData(String containerName, String blobPathAndName);

    /**
     * Deletes the specified blob in the specified path blobPathAndName from the root container.
     *
     * @param containerName   name of blob client container
     * @param blobPathAndName name of blob in response folder to be deleted
     * @return true if the blob was successfully deleted otherwise false
     */
    boolean deleteBlobData(String containerName, String blobPathAndName);

}
