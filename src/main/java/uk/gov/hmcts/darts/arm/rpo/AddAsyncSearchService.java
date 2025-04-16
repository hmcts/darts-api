package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@FunctionalInterface
public interface AddAsyncSearchService {

    String addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
