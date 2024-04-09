package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;

import java.time.OffsetDateTime;
import java.util.List;

public interface ArmDataManagementApi extends BlobContainerDownloadable {

    String saveBlobDataToArm(String filename, BinaryData binaryData);

    List<String> listResponseBlobs(String prefix);

    List<String> listResponseBlobsUsingBatch(String prefix);

    ContinuationTokenBlobs listResponseBlobsUsingMarker(String prefix, String continuationToken);

    BinaryData getBlobData(String blobPathAndName);

    boolean deleteBlobData(String blobPathAndName);

    UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp);
}