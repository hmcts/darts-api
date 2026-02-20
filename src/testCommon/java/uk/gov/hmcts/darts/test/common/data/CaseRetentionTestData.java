package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCaseRetentionEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTypeTestData.someMinimalRetentionPolicyType;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class CaseRetentionTestData implements Persistable<TestCaseRetentionEntity.TestCaseRetentionBuilderRetrieve,
    CaseRetentionEntity, TestCaseRetentionEntity.TestCaseRetentionEntityBuilder> {

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

    @Override
    public CaseRetentionEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestCaseRetentionEntity.TestCaseRetentionEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestCaseRetentionEntity.TestCaseRetentionBuilderRetrieve someMinimalBuilderHolder() {
        TestCaseRetentionEntity.TestCaseRetentionBuilderRetrieve retrieve
            = new TestCaseRetentionEntity.TestCaseRetentionBuilderRetrieve();
        retrieve.getBuilder().courtCase(PersistableFactory.getCourtCaseTestData().someMinimalCase())
            .retentionPolicyType(someMinimalRetentionPolicyType())
            .retainUntil(OffsetDateTime.now().plusYears(7))
            .currentState("some-state")
            .createdDateTime(OffsetDateTime.now())
            .createdById(0)
            .submittedBy(minimalUserAccount())
            .lastModifiedDateTime(OffsetDateTime.now())
            .lastModifiedById(0);
        return retrieve;
    }
}