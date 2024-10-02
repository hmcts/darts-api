package uk.gov.hmcts.darts.dets.api;

import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;

public interface DetsDataManagementApi extends BlobContainerDownloadable {

    void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName);
}