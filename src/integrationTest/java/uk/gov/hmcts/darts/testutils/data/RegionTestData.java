package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;

import java.util.Random;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class RegionTestData {

    private static final Random RANDOM = new Random();

    public static RegionEntity minimalRegion() {
        var postfix = RANDOM.nextInt(1000, 9999);
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region-" + postfix);
        return regionEntity;
    }
}
