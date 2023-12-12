package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.ArmBlobInfo;
import uk.gov.hmcts.darts.arm.service.ArmService;

@Service
@RequiredArgsConstructor
public class ArmDataManagementApiImpl implements ArmDataManagementApi {

    private final ArmService armService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public ArmBlobInfo saveBlobDataToArm(String filename, BinaryData binaryData) {
        return armService.saveBlobData(getArmContainerName(), filename, binaryData);
    }


    private String getArmContainerName() {
        return armDataManagementConfiguration.getArmContainerName();
    }


}
