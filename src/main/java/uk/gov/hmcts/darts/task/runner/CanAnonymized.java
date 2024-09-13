package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.UUID;

public interface CanAnonymized {

    void anonymize(UserAccountEntity userAccount, UUID uuid);
}
