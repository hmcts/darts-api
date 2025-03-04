package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

public final class ObjectRecordStatusTestData {

    private ObjectRecordStatusTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static ObjectRecordStatusEntity statusOf(ObjectRecordStatusEnum status) {
        var objectRecordStatus = new ObjectRecordStatusEntity();
        objectRecordStatus.setId(status.getId());
        return objectRecordStatus;
    }
}