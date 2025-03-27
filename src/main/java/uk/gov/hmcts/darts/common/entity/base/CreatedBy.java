package uk.gov.hmcts.darts.common.entity.base;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public interface CreatedBy {

    void setCreatedBy(UserAccountEntity userAccount);

    Integer getCreatedById();

    void setCreatedById(Integer createdById);

    OffsetDateTime getCreatedDateTime();

    void setCreatedDateTime(OffsetDateTime now);

    boolean isSkipUserAudit();

    void setSkipUserAudit(boolean value);

}
