package uk.gov.hmcts.darts.arm.mapper.template;


import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

public class AnnotationRecordTemplateMapper extends BaseTemplate {

    public AnnotationRecordTemplateMapper(ArmDataManagementConfiguration armDataManagementConfiguration,
                                          CurrentTimeHelper currentTimeHelper) {
        super(armDataManagementConfiguration, currentTimeHelper);
    }
}
