package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;

import java.util.Map;

public interface ArmService {
    String saveBlobData(String containerName, String filename, BinaryData binaryData);

    String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName);

    Map<String, BlobItem> listSubmissionBlobs(String containerName, String filename);

    Map<String, BlobItem> listCollectedBlobs(String containerName, String filename);

    Map<String, BlobItem> listResponseBlobs(String containerName, String filename);

    BinaryData getBlobData(String containerName, String blobName);
}
