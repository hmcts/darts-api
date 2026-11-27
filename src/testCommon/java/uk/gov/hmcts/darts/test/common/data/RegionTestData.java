package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;

import static org.apache.commons.lang3.RandomStringUtils.secure;


public final class RegionTestData {

    private RegionTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static RegionEntity minimalRegion() {
        var postfix = secure().next(10);
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region-" + postfix);
        return regionEntity;
    }
}