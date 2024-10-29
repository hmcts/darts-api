package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface ArmRpoService {

    ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId);

    void updateArmRpoStateAndStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStateEntity armRpoStateEntity,
                                    ArmRpoStatusEntity armRpoStatusEntity, UserAccountEntity userAccountEntity);

    void updateArmRpoStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStatusEntity armRpoStatusEntity,
                            UserAccountEntity userAccountEntity);

    ArmRpoExecutionDetailEntity saveArmRpoExecutionDetailEntity(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity);
}
