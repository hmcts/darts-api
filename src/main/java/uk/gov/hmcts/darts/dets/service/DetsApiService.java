package uk.gov.hmcts.darts.dets.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

public interface DetsApiService {
    DownloadResponseMetaData downloadData(String blobId) throws FileNotDownloadedException;

    String saveBlobData(BinaryData binaryData);

    boolean deleteBlobDataFromContainer(String blobId) throws AzureDeleteBlobException;

    void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName);
}