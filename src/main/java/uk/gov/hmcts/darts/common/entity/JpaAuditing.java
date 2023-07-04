package uk.gov.hmcts.darts.common.entity;

import java.time.OffsetDateTime;

public interface JpaAuditing {

    OffsetDateTime getCreatedTimestamp();

    void setCreatedTimestamp(OffsetDateTime createdTimestamp);

    OffsetDateTime getModifiedTimestamp();

    void setModifiedTimestamp(OffsetDateTime modifiedTimestamp);

    Integer getModifiedBy();

    void setModifiedBy(Integer modifiedBy);

}
