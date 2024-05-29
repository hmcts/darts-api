package uk.gov.hmcts.darts.testutils;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ObjectRecordStatusTestData {

    public static ObjectRecordStatusEntity getObjectRecordStatus(ObjectRecordStatusEnum objectRecordStatusEnum) {
        ObjectRecordStatusEntity objectRecordStatus = new ObjectRecordStatusEntity();
        objectRecordStatus.setId(objectRecordStatusEnum.getId());
        objectRecordStatus.setDescription(objectRecordStatusEnum.name());
        return objectRecordStatus;
    }
}
