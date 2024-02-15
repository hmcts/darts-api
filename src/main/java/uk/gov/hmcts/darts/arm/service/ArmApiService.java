package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;

import java.io.InputStream;
import java.time.OffsetDateTime;

public interface ArmApiService {

    UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp);

    InputStream downloadArmData(String externalRecordId, String externalFileId, DownloadResponseMetaData responseMetaData);
}