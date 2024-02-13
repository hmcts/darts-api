package uk.gov.hmcts.darts.dets.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;

import java.util.UUID;

public interface DetsApiService {
    void downloadData(UUID blobId, DownloadResponseMetaData response);

    UUID saveBlobData(BinaryData binaryData);
}