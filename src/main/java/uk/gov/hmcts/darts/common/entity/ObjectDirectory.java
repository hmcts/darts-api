package uk.gov.hmcts.darts.common.entity;

import java.util.UUID;

public interface ObjectDirectory {

    int getStatusId();

    UUID getLocation();

    void setStatus(ObjectRecordStatusEntity deletedStatus);

    void setLastModifiedBy(UserAccountEntity systemUser);

    Integer getId();
}
