package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

@Service
@AllArgsConstructor
public class ArmRpoServiceImpl implements ArmRpoService {

    public static final String ARM_RPO_EXECUTION_DETAIL_NOT_FOUND = "ArmRpoExecutionDetail not found";
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    @Override
    public ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId) {
        return armRpoExecutionDetailRepository.findById(executionId).orElseThrow(() -> new DartsException(ARM_RPO_EXECUTION_DETAIL_NOT_FOUND));
    }

    @Override
    public void updateArmRpoStateAndStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStateEntity armRpoStateEntity,
                                           ArmRpoStatusEntity armRpoStatusEntity, UserAccountEntity userAccountEntity) {
        armRpoExecutionDetailEntity.setArmRpoState(armRpoStateEntity);
        updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoStatusEntity, userAccountEntity);
    }

    @Override
    public void updateArmRpoStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStatusEntity armRpoStatusEntity,
                                   UserAccountEntity userAccountEntity) {
        armRpoExecutionDetailEntity.setArmRpoStatus(armRpoStatusEntity);
        armRpoExecutionDetailEntity.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccountEntity);
        saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);
    }

    @Override
    public ArmRpoExecutionDetailEntity saveArmRpoExecutionDetailEntity(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
    }
}
