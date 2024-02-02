package uk.gov.hmcts.darts.arm.mapper.template;

import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

public class CaseRecordTemplateMapper extends BaseTemplate {

    public CaseRecordTemplateMapper(ArmDataManagementConfiguration armDataManagementConfiguration,
                                    CurrentTimeHelper currentTimeHelper) {
        super(armDataManagementConfiguration, currentTimeHelper);
    }
}
