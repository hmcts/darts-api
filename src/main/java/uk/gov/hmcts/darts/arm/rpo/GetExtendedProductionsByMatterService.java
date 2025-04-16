package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@FunctionalInterface
public interface GetExtendedProductionsByMatterService {

    boolean getExtendedProductionsByMatter(String bearerToken, Integer executionId, String productionName, UserAccountEntity userAccount);

}
