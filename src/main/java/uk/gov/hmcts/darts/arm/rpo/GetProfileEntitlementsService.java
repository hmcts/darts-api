package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@FunctionalInterface
public interface GetProfileEntitlementsService {

    void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
