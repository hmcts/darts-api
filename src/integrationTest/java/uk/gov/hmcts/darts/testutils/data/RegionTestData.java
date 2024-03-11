package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class RegionTestData {

    public static RegionEntity minimalRegion() {
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region");
        return regionEntity;
    }
}
