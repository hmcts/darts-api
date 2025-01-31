package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;

import java.time.OffsetDateTime;

public class RetentionConfidenceCategoryMapperTestData
    implements Persistable<TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperRetrieve,
    RetentionConfidenceCategoryMapperEntity,
    TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder> {


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
            .createdById(TestUtils.AUTOMATION_USER_ID)
            .lastModifiedAt(someDateTime)
            .lastModifiedById(TestUtils.AUTOMATION_USER_ID);

        return retrieve;
    }

    @Override
    public TestRetentionConfidenceCategoryMapperEntity.TestRetentionConfidenceCategoryMapperEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

}
