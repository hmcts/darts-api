package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTypeTestData.someMinimalRetentionPolicyType;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class CaseRetentionTestData {

    private CaseRetentionTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static CaseRetentionEntity someMinimalCaseRetention() {

        var caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(PersistableFactory.getCourtCaseTestData().someMinimalCase());
        caseRetention.setRetentionPolicyType(someMinimalRetentionPolicyType());
        caseRetention.setRetainUntil(OffsetDateTime.now().plusYears(7));
        caseRetention.setCurrentState("some-state");
        caseRetention.setCreatedDateTime(OffsetDateTime.now());
        caseRetention.setCreatedById(0);
        caseRetention.setSubmittedBy(minimalUserAccount());
        caseRetention.setLastModifiedDateTime(OffsetDateTime.now());
        caseRetention.setLastModifiedById(0);
        return caseRetention;
    }

    public static CaseRetentionEntity createCaseRetentionFor(CaseManagementRetentionEntity caseManagementRetention) {
        var caseRetentionEntity = someMinimalCaseRetention();
        caseRetentionEntity.setCaseManagementRetention(caseManagementRetention);
        caseRetentionEntity.setCourtCase(caseManagementRetention.getCourtCase());
        return caseRetentionEntity;
    }

}