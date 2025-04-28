package uk.gov.hmcts.darts.common.entity;

public interface ObjectDirectory {
    int getStatusId();

    String getLocation();

    void setStatus(ObjectRecordStatusEntity deletedStatus);

    void setLastModifiedBy(UserAccountEntity systemUser);

    Long getId();
}
