package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;

public final class ArmRpoStatusEntityTestData {

    private ArmRpoStatusEntityTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static ArmRpoStatusEntity statusOf(ArmRpoStatusEnum status) {
        var armRpoStatusEntity = new ArmRpoStatusEntity();
        armRpoStatusEntity.setId(status.getId());
        armRpoStatusEntity.setDescription(status.name());
        return armRpoStatusEntity;
    }
}
