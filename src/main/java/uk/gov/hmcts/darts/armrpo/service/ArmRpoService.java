package uk.gov.hmcts.darts.armrpo.service;

import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface ArmRpoService {

    void updateArmRpoExecutionDetails(ArmRpoStatusEntity armRpoStatus, OffsetDateTime lastModifiedDateTime, UserAccountEntity lastModifiedBy);
}
