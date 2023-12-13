package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;

public interface ArmService {
    String saveBlobData(String containerName, String filename, BinaryData binaryData);
}
