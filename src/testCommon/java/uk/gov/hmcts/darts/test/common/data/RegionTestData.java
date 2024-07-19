package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RegionEntity;

import static org.apache.commons.lang3.RandomStringUtils.random;


@SuppressWarnings({"HideUtilityClassConstructor"})
public class RegionTestData {

    public static RegionEntity minimalRegion() {
        var postfix = random(10);
        var regionEntity = new RegionEntity();
        regionEntity.setRegionName("some-region-" + postfix);
        return regionEntity;
    }
}
