package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;

import java.time.OffsetDateTime;
import java.util.List;

public interface ArmDataManagementApi extends BlobContainerDownloadable {

    String saveBlobDataToArm(String filename, BinaryData binaryData);

    List<String> listCollectedBlobs(String prefix);

    List<String> listResponseBlobs(String prefix);

    BinaryData getBlobData(String blobName);

    void deleteResponseBlob(String blobName);

    UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp);
}