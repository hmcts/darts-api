package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class RegionTestData {

    public static RegionEntity minimalRegion() {
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region");
        return regionEntity;
    }
}
