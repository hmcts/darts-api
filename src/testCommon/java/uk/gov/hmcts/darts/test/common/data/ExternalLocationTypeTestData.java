package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class ExternalLocationTypeTestData {

    public static ExternalLocationTypeEntity locationTypeOf(ExternalLocationTypeEnum locationType) {
        var externalLocationType = new ExternalLocationTypeEntity();
        externalLocationType.setId(locationType.getId());
        return externalLocationType;
    }
}
