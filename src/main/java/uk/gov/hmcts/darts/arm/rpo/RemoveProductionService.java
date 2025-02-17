package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface RemoveProductionService {

    void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
