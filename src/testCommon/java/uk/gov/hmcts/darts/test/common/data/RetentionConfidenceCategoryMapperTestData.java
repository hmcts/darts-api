package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;

import java.time.OffsetDateTime;

public class RetentionConfidenceCategoryMapperTestData
    implements Persistable<TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperRetrieve,
    RetentionConfidenceCategoryMapperEntity,
    TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder> {

    private static final UserAccountEntity USER_ACCOUNT = UserAccountTestData.minimalUserAccount();

    @Override
    public RetentionConfidenceCategoryMapperEntity someMinimal() {
        return someMinimalBuilderHolder().getBuilder()
            .build();
    }

    @Override
    public TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperRetrieve someMinimalBuilderHolder() {
        final var retrieve = new TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperRetrieve();

        final OffsetDateTime someDateTime = OffsetDateTime.now();

        retrieve.getBuilder()
            .createdAt(someDateTime)
            .createdBy(USER_ACCOUNT)
            .lastModifiedAt(someDateTime)
            .lastModifiedBy(USER_ACCOUNT);

        return retrieve;
    }

    @Override
    public TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

}
