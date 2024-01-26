package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.List;

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
    public List<String> listCollectedBlobs(String prefix) {
        return armService.listCollectedBlobs(getArmContainerName(), prefix);
    }

    @Override
    public List<String> listResponseBlobs(String prefix) {
        return armService.listResponseBlobs(getArmContainerName(), prefix);
    }

    public BinaryData getResponseBlobData(String blobName) {
        return armService.getBlobData(
            getArmContainerName(),
            armDataManagementConfiguration.getFolders().getResponse() + blobName
        );
    }

    @Override
    public void deleteResponseBlob(String blobName) {
        armService.deleteResponseBlob(getArmContainerName(), blobName);
    }

    private String getArmContainerName() {
        return armDataManagementConfiguration.getContainerName();
    }
}
