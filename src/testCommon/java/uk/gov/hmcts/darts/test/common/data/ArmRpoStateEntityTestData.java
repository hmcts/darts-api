package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;

public final class ArmRpoStateEntityTestData {

    private ArmRpoStateEntityTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static ArmRpoStateEntity stateOf(ArmRpoStateEnum state) {
        var armRpoStateEntity = new ArmRpoStateEntity();
        armRpoStateEntity.setId(state.getId());
        armRpoStateEntity.setDescription(state.name());
        return armRpoStateEntity;
    }
}
