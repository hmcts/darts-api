package uk.gov.hmcts.darts.armrpo.service;

import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;

public interface ArmRpoService {

    ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId);

    void updateArmRpoExecutionDetails(Integer executionId, ArmRpoStatusEntity armRpoStatus);


}
