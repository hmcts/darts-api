package uk.gov.hmcts.darts.dets.service;

import uk.gov.hmcts.darts.common.datamanagement.component.impl.ResponseMetaData;

import java.util.UUID;

public interface DetsApiService {
    void downloadData(UUID blobId, ResponseMetaData response);
}