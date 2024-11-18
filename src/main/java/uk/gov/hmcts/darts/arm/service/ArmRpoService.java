package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.File;
import java.util.List;

public interface ArmRpoService {

    ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(UserAccountEntity userAccount);

    ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId);

    void updateArmRpoStateAndStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStateEntity armRpoStateEntity,
                                    ArmRpoStatusEntity armRpoStatusEntity, UserAccountEntity userAccountEntity);

    void updateArmRpoStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStatusEntity armRpoStatusEntity,
                            UserAccountEntity userAccountEntity);

    ArmRpoExecutionDetailEntity saveArmRpoExecutionDetailEntity(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity);

    void reconcileArmRpoCsvData(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, List<File> csvFiles);

}
