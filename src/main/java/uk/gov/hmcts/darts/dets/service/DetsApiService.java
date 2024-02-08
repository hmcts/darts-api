package uk.gov.hmcts.darts.dets.service;

import uk.gov.hmcts.darts.common.datamanagement.component.MediaDownloadMetaData;

import java.util.UUID;

public interface DetsApiService {
    void downloadData(UUID blobId, MediaDownloadMetaData report);
}