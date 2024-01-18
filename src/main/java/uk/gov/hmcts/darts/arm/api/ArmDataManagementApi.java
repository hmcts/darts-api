package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;

import java.util.Map;

public interface ArmDataManagementApi {

    String saveBlobDataToArm(String filename, BinaryData binaryData);

    Map<String, BlobItem> listCollectedBlobs(String prefix);

    Map<String, BlobItem> listResponseBlobs(String prefix);

    BinaryData getResponseBlobData(String blobName);

    void deleteResponseBlob(String blobName);
}
