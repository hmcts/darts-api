package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;

import java.util.List;

public interface ArmService {
    String saveBlobData(String containerName, String filename, BinaryData binaryData);

    String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName);

    List<String> listSubmissionBlobs(String containerName, String filename);

    List<String> listCollectedBlobs(String containerName, String filename);

    List<String> listResponseBlobs(String containerName, String filename);

    BinaryData getBlobData(String containerName, String blobName);

    boolean deleteResponseBlob(String containerName, String blobPathAndName);

}
