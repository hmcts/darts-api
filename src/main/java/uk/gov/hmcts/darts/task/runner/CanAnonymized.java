package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface CanAnonymized {

    void anonymize(UserAccountEntity userAccount);
}
