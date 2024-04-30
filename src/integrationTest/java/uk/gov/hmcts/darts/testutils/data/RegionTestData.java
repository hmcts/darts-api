package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;

import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.random;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class RegionTestData {

    public static RegionEntity minimalRegion() {
        var postfix = random(10);
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region-" + postfix);
        return regionEntity;
    }
}
