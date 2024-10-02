package uk.gov.hmcts.darts.dets.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.util.UUID;

public interface DetsApiService {
    DownloadResponseMetaData downloadData(UUID blobId) throws FileNotDownloadedException;

    UUID saveBlobData(BinaryData binaryData);

    void copyDetsBlobDataToArm(String detsUuid, String blobPathAndName);
}