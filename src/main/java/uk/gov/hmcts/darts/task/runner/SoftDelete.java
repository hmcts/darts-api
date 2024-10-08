package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

/**
 * Interface for entities that support soft delete.
 * To support abstraction of deletion logic
 */
public interface SoftDelete {

    default void markAsDeleted(UserAccountEntity userAccount) {
        setDeletedBy(userAccount);
        setDeletedTs(OffsetDateTime.now());
        setDeleted(true);
    }

    void setDeleted(boolean deleted);

    boolean isDeleted();

    void setDeletedTs(OffsetDateTime deletedTs);

    OffsetDateTime getDeletedTs();

    void setDeletedBy(UserAccountEntity userAccount);

    UserAccountEntity getDeletedBy();

}
