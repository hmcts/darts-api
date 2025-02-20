package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface GetStorageAccountsService {

    void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
