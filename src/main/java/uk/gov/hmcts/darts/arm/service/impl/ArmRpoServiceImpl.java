package uk.gov.hmcts.darts.arm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoServiceImpl implements ArmRpoService {

    public static final String ARM_RPO_EXECUTION_DETAIL_NOT_FOUND = "ArmRpoExecutionDetail not found";
    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();

        UserAccountEntity mergedUserAccountEntity = entityManager.merge(userAccount);
        armRpoExecutionDetailEntity.setCreatedBy(mergedUserAccountEntity);
        armRpoExecutionDetailEntity.setLastModifiedBy(mergedUserAccountEntity);

        return saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);
    }

    @Override
    public ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId) {
        return armRpoExecutionDetailRepository.findById(executionId).orElseThrow(() -> new DartsException(ARM_RPO_EXECUTION_DETAIL_NOT_FOUND));
    }

    @Override
    public void updateArmRpoStateAndStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStateEntity armRpoStateEntity,
                                           ArmRpoStatusEntity armRpoStatusEntity, UserAccountEntity userAccountEntity) {
        String previousState = nonNull(armRpoExecutionDetailEntity.getArmRpoState()) ? armRpoExecutionDetailEntity.getArmRpoState().getDescription() : null;
        log.debug("Setting execution detail {} state from {} to {}", armRpoExecutionDetailEntity.getId(),
                  previousState,
                  armRpoStateEntity.getDescription());
        armRpoExecutionDetailEntity.setArmRpoState(armRpoStateEntity);
        updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoStatusEntity, userAccountEntity);
    }

    @Override
    public void updateArmRpoStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStatusEntity armRpoStatusEntity,
                                   UserAccountEntity userAccountEntity) {
        String previousStatus = nonNull(armRpoExecutionDetailEntity.getArmRpoStatus()) ? armRpoExecutionDetailEntity.getArmRpoStatus().getDescription() : null;
        log.debug("Setting execution detail {} status from {} to {}", armRpoExecutionDetailEntity.getId(),
                  previousStatus,
                  armRpoStatusEntity.getDescription());
        armRpoExecutionDetailEntity.setArmRpoStatus(armRpoStatusEntity);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccountEntity);
        saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);
    }

    @Override
    public ArmRpoExecutionDetailEntity saveArmRpoExecutionDetailEntity(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
    }

}
