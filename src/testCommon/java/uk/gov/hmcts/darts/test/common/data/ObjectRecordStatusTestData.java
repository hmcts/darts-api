package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class ObjectRecordStatusTestData {

    public static ObjectRecordStatusEntity statusOf(ObjectRecordStatusEnum status) {
        var objectRecordStatus = new ObjectRecordStatusEntity();
        objectRecordStatus.setId(status.getId());
        return objectRecordStatus;
    }
}
