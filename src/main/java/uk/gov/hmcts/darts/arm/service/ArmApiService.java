package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.time.OffsetDateTime;

public interface ArmApiService {

    UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp, int retConfScore, String retConfReason);

    DownloadResponseMetaData downloadArmData(String externalRecordId, String externalFileId) throws FileNotDownloadedException;

    String getArmBearerToken();
}