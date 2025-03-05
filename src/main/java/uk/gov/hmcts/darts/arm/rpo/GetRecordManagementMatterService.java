package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface GetRecordManagementMatterService {

    void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);
    
}
