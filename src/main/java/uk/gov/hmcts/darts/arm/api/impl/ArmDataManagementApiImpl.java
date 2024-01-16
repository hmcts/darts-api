package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArmDataManagementApiImpl implements ArmDataManagementApi {

    private final ArmService armService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public String saveBlobDataToArm(String filename, BinaryData binaryData) {
        return armService.saveBlobData(getArmContainerName(), filename, binaryData);
    }

    @Override
    public Map<String, BlobItem> listCollectedBlobs(String prefix) {
        String filename = armDataManagementConfiguration.getFolders().getCollected() + prefix;
        return armService.listCollectedBlobs(getArmContainerName(), filename);
    }

    @Override
    public Map<String, BlobItem> listResponseBlobs(String prefix) {
        return armService.listResponseBlobs(
            getArmContainerName(),
            armDataManagementConfiguration.getFolders().getResponse() + prefix
        );
    }

    public BinaryData getResponseBlobData(String blobName) {
        return armService.getBlobData(
            getArmContainerName(),
            armDataManagementConfiguration.getFolders().getResponse() + blobName
        );
    }

    private String getArmContainerName() {
        return armDataManagementConfiguration.getContainerName();
    }
}
