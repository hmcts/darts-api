package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface SoftDelete {

    default void markAsDeleted(UserAccountEntity userAccount) {
        setDeletedBy(userAccount);
        setDeletedTs(OffsetDateTime.now());
        setDeleted(true);
    }

    void setDeleted(boolean deleted);

    void setDeletedTs(OffsetDateTime deletedTs);

    void setDeletedBy(UserAccountEntity userAccount);
}
