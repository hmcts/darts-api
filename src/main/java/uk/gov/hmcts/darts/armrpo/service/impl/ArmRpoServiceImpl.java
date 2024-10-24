package uk.gov.hmcts.darts.armrpo.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.armrpo.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
public class ArmRpoServiceImpl implements ArmRpoService {

    public static final String ARM_RPO_EXECUTION_DETAIL_NOT_FOUND = "ArmRpoExecutionDetail not found";
    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    @Override
    public ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId) {
        return armRpoExecutionDetailRepository.findById(executionId).orElseThrow(() -> new DartsException(ARM_RPO_EXECUTION_DETAIL_NOT_FOUND));
    }

    @Override
    public void updateArmRpoExecutionDetails(Integer executionId, ArmRpoStatusEntity armRpoStatus, OffsetDateTime lastModifiedDateTime,
                                             UserAccountEntity lastModifiedBy) {
        var armRpoExecutionDetailEntity = getArmRpoExecutionDetailEntity(executionId);
        armRpoExecutionDetailEntity.setArmRpoStatus(armRpoStatus);
        armRpoExecutionDetailEntity.setLastModifiedDateTime(lastModifiedDateTime);
        armRpoExecutionDetailEntity.setLastModifiedBy(lastModifiedBy);
        armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
    }


}
