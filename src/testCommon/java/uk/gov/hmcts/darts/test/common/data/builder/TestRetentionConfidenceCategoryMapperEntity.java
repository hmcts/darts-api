package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestRetentionConfidenceCategoryMapperEntity extends RetentionConfidenceCategoryMapperEntity
    implements DbInsertable<RetentionConfidenceCategoryMapperEntity> {

    @Builder
    public TestRetentionConfidenceCategoryMapperEntity(Integer id, RetentionConfidenceScoreEnum confidenceScore,
                                                       RetentionConfidenceReasonEnum confidenceReason,
                                                       RetentionConfidenceCategoryEnum confidenceCategory,
                                                       String description, OffsetDateTime createdAt,
                                                       OffsetDateTime lastModifiedAt, UserAccountEntity createdBy,
                                                       UserAccountEntity lastModifiedBy) {
        super();
        setId(id);
        setConfidenceScore(confidenceScore);
        setConfidenceReason(confidenceReason);
        setConfidenceCategory(confidenceCategory);
        setDescription(description);
        setCreatedDateTime(createdAt);
        setLastModifiedDateTime(lastModifiedAt);
        setCreatedBy(createdBy);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public RetentionConfidenceCategoryMapperEntity getEntity() {
        try {
            RetentionConfidenceCategoryMapperEntity retentionConfidenceCategoryMapperEntity = new RetentionConfidenceCategoryMapperEntity();
            BeanUtils.copyProperties(retentionConfidenceCategoryMapperEntity, this);
            return retentionConfidenceCategoryMapperEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestRetentionConfidenceCategoryMapperRetrieve
        implements BuilderHolder<TestRetentionConfidenceCategoryMapperEntity,
        TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder> {

        private final TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder builder =
            TestRetentionConfidenceCategoryMapperEntity.builder();

        @Override
        public TestRetentionConfidenceCategoryMapperEntity build() {
            return builder.build();
        }

        @Override
        public TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder getBuilder() {
            return builder;
        }
    }

}
