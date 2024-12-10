package uk.gov.hmcts.darts.common.entity.base;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface LastModifiedBy {
    UserAccountEntity getLastModifiedBy();

    void setLastModifiedBy(UserAccountEntity userAccount);

    void setLastModifiedDateTime(OffsetDateTime now);

    boolean isSkipUserAudit();

    void setSkipUserAudit(boolean value);
}
