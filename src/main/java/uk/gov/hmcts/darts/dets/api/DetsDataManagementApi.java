package uk.gov.hmcts.darts.dets.api;

import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.UUID;

public interface DetsDataManagementApi extends BlobContainerDownloadable {

    void deleteBlobDataFromContainer(UUID blobId) throws AzureDeleteBlobException;

    String getChecksum(UUID guid);

    void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName);

}