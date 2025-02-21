package uk.gov.hmcts.darts.dets.api;

import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

public interface DetsDataManagementApi extends BlobContainerDownloadable {

    void deleteBlobDataFromContainer(String blobId) throws AzureDeleteBlobException;

    String getChecksum(String guid);

    void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName);

}