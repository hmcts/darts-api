package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

public final class ObjectRecordStatusTestData {

    private ObjectRecordStatusTestData() {

    }

    public static ObjectRecordStatusEntity statusOf(ObjectRecordStatusEnum status) {
        var objectRecordStatus = new ObjectRecordStatusEntity();
        objectRecordStatus.setId(status.getId());
        return objectRecordStatus;
    }
}