package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

public interface UnstructuredToArmProcessor {
    void processUnstructuredToArm();

    String generateFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity);
}
