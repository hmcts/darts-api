package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

public interface ObjectRecordStatusService {
    ObjectRecordStatusEntity getObjectRecordStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum);
}
