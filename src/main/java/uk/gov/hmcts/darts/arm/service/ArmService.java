package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.arm.model.ArmBlobInfo;

public interface ArmService {
    ArmBlobInfo saveBlobData(String containerName, String filename, BinaryData binaryData);
}
