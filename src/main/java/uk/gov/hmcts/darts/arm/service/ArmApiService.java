package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;

import java.io.InputStream;
import java.time.OffsetDateTime;

public interface ArmApiService {

    UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp);

    InputStream downloadArmData(String externalRecordId, String externalFileId);
}
