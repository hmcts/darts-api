package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArmDataManagementApiImpl implements ArmDataManagementApi {

    private final ArmService armService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmApiService armApiService;

    @Override
    public String saveBlobDataToArm(String filename, BinaryData binaryData) {
        return armService.saveBlobData(getArmContainerName(), filename, binaryData);
    }

    @Override
    public List<String> listCollectedBlobs(String prefix) {
        return armService.listCollectedBlobs(getArmContainerName(), prefix);
    }

    @Override
    public List<String> listResponseBlobs(String prefix) {
        return armService.listResponseBlobs(getArmContainerName(), prefix);
    }

    public BinaryData getBlobData(String blobPathAndName) {
        return armService.getBlobData(
              getArmContainerName(),
              blobPathAndName
        );
    }

    @Override
    public void deleteResponseBlob(String blobName) {
        armService.deleteResponseBlob(getArmContainerName(), blobName);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp) {
        return armApiService.updateMetadata(externalRecordId, eventTimestamp);
    }

    @Override
    public InputStream downloadArmData(String externalRecordId, String externalFileId) {
        return armApiService.downloadArmData(externalRecordId, externalFileId);
    }

    private String getArmContainerName() {
        return armDataManagementConfiguration.getContainerName();
    }
}
