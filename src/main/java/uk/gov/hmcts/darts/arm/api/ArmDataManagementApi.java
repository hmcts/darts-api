package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.model.ArmBlobInfo;

public interface ArmDataManagementApi {

    ArmBlobInfo saveBlobDataToArm(String filename, BinaryData binaryData);
}
