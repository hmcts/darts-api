package uk.gov.hmcts.darts.common.entity.base;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface LastModifiedBy {
    void setLastModifiedBy(UserAccountEntity userAccount);

    void setLastModifiedDateTime(OffsetDateTime now);

    boolean isSkipUserAudit();

    void setSkipUserAudit(boolean value);

    OffsetDateTime getLastModifiedDateTime();

    void setLastModifiedById(Integer lastModifiedById);

    Integer getLastModifiedById();
}
