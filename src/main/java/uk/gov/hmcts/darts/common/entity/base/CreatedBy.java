package uk.gov.hmcts.darts.common.entity.base;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface CreatedBy {
    UserAccountEntity getCreatedBy();

    void setCreatedBy(UserAccountEntity userAccount);

    void setCreatedDateTime(OffsetDateTime now);

    boolean isSkipUserAudit();
}
