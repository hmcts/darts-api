package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface GetExtendedSearchesByMatterService {

    String getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
