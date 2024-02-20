package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

public class ExternalLocationTypeTestData {

    public static ExternalLocationTypeEntity getExternalLocationType(ExternalLocationTypeEnum externalLocationTypeEnum) {
        ExternalLocationTypeEntity externalLocationType = new ExternalLocationTypeEntity();
        externalLocationType.setId(externalLocationTypeEnum.getId());
        externalLocationType.setDescription(externalLocationType.getDescription());
        return externalLocationType;
    }

}
