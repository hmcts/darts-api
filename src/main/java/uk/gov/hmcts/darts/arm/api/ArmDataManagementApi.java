package uk.gov.hmcts.darts.arm.api;

import com.azure.core.util.BinaryData;

public interface ArmDataManagementApi {

    String saveBlobDataToARM(String filename, BinaryData binaryData);
}
