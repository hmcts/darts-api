package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;

import static uk.gov.hmcts.darts.test.common.data.EventTestData.someMinimalEvent;
import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTypeTestData.someMinimalRetentionPolicyType;


public final class CaseManagementRetentionTestData {

    private CaseManagementRetentionTestData() {
    }

    public static CaseManagementRetentionEntity someMinimalCaseManagementRetention() {
        var caseManagementRetention = new CaseManagementRetentionEntity();
        caseManagementRetention.setEventEntity(someMinimalEvent());
        caseManagementRetention.setCourtCase(PersistableFactory.getCourtCaseTestData().someMinimalCase());
        caseManagementRetention.setRetentionPolicyTypeEntity(someMinimalRetentionPolicyType());
        return caseManagementRetention;
    }
}