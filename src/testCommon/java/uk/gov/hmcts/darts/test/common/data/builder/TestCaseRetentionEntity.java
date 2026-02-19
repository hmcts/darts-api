package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestCaseRetentionEntity extends CaseRetentionEntity implements DbInsertable<CaseRetentionEntity> {

    @lombok.Builder
    public TestCaseRetentionEntity(
        Integer id,
        CourtCaseEntity courtCase,
        RetentionPolicyTypeEntity retentionPolicyType,
        CaseManagementRetentionEntity caseManagementRetention,
        String totalSentence,
        OffsetDateTime retainUntil,
        OffsetDateTime retainUntilAppliedOn,
        String currentState,
        String comments,
        RetentionConfidenceCategoryEnum confidenceCategory,
        String retentionObjectId,
        UserAccountEntity submittedBy,
        OffsetDateTime createdDateTime,
        Integer createdById,
        OffsetDateTime lastModifiedDateTime,
        Integer lastModifiedById
    ) {
        super();
        setId(id);
        setCourtCase(courtCase);
        setRetentionPolicyType(retentionPolicyType);
        setCaseManagementRetention(caseManagementRetention);
        setTotalSentence(totalSentence);
        setRetainUntil(retainUntil);
        setRetainUntilAppliedOn(retainUntilAppliedOn);
        setCurrentState(currentState);
        setComments(comments);
        setConfidenceCategory(confidenceCategory);
        setRetentionObjectId(retentionObjectId);
        setSubmittedBy(submittedBy);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public CaseRetentionEntity getEntity() {
        try {
            CaseRetentionEntity entity = new CaseRetentionEntity();
            BeanUtils.copyProperties(entity, this);
            return entity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestCaseRetentionBuilderRetrieve
        implements BuilderHolder<TestCaseRetentionEntity, TestCaseRetentionEntity.TestCaseRetentionEntityBuilder> {

        private final TestCaseRetentionEntity.TestCaseRetentionEntityBuilder builder = TestCaseRetentionEntity.builder();

        @Override
        public TestCaseRetentionEntity build() {
            return builder.build();
        }

        @Override
        public TestCaseRetentionEntity.TestCaseRetentionEntityBuilder getBuilder() {
            return builder;
        }
    }
}
