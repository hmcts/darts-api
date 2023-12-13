package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;

public interface ArmDataManagementApi {

    String saveBlobDataToArm(String filename, BinaryData binaryData);
}
