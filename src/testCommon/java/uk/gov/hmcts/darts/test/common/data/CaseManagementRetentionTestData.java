package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;

import static uk.gov.hmcts.darts.test.common.data.CaseTestData.someMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someMinimalEvent;
import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTypeTestData.someMinimalRetentionPolicyType;


@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CaseManagementRetentionTestData {

    public static CaseManagementRetentionEntity
    someMinimalCaseManagementRetention() {
        var caseManagementRetention = new CaseManagementRetentionEntity();
        caseManagementRetention.setEventEntity(someMinimalEvent());
        caseManagementRetention.setCourtCase(someMinimalCase());
        caseManagementRetention.setRetentionPolicyTypeEntity(someMinimalRetentionPolicyType());
        return caseManagementRetention;
    }
}
