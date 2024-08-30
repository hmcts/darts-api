package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface JudgeCommonService {
    JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount);
}
