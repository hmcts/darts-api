package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.time.OffsetDateTime;
import java.util.List;

public interface ArmDataManagementApi extends BlobContainerDownloadable {

    String saveBlobDataToArm(String filename, BinaryData binaryData);

    void copyBlobDataToArm(String unstructuredUuid, String filename);

    void copyDetsBlobDataToArm(String detsUuid, String filename);

    List<String> listResponseBlobs(String prefix);

    List<String> listResponseBlobsUsingBatch(String prefix, int batchSize);

    ContinuationTokenBlobs listResponseBlobsUsingMarker(String prefix, int batchSize, String continuationToken);

    BinaryData getBlobData(String blobPathAndName);

    boolean deleteBlobData(String blobPathAndName);

    UpdateMetadataResponse updateMetadata(String externalRecordId,
                                          OffsetDateTime eventTimestamp,
                                          RetentionConfidenceScoreEnum retConfScore,
                                          String retConfReason);

    default UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp, String retConfReason) {
        return updateMetadata(externalRecordId, eventTimestamp, null, retConfReason);
    }
}