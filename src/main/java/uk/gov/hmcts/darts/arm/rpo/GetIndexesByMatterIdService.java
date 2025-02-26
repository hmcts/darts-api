package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface GetIndexesByMatterIdService {

    void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount);
    
}
