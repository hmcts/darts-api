package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface SaveBackgroundSearchService {

    void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount);

}
