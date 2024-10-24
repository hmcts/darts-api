package uk.gov.hmcts.darts.armrpo.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.armrpo.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@Service
public class ArmRpoServiceImpl implements ArmRpoService {

    @Override
    public void updateArmRpoExecutionDetails(ArmRpoStatusEntity armRpoStatus, OffsetDateTime lastModifiedDateTime, UserAccountEntity lastModifiedBy) {

    }
}
