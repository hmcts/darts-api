package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;

import java.util.List;

public interface ArmDataManagementApi {

    String saveBlobDataToArm(String filename, BinaryData binaryData);

    List<String> listCollectedBlobs(String prefix);

    List<String> listResponseBlobs(String prefix);

    BinaryData getBlobData(String blobName);

    void deleteResponseBlob(String blobName);
}
