package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;

public class ArmRpoStatusEntityTestData {

    private ArmRpoStatusEntityTestData() {
    }

    public static ArmRpoStatusEntity statusOf(ArmRpoStatusEnum state) {
        var armRpoStatusEntity = new ArmRpoStatusEntity();
        armRpoStatusEntity.setId(state.getId());
        armRpoStatusEntity.setDescription(state.name());
        return armRpoStatusEntity;
    }
}
