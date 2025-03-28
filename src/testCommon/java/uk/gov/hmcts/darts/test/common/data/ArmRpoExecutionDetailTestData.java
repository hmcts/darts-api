package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestArmRpoExecutionDetailEntity;

import java.time.OffsetDateTime;

public final class ArmRpoExecutionDetailTestData implements Persistable<TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilderRetrieve,
    ArmRpoExecutionDetailEntity, TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilder> {

    ArmRpoExecutionDetailTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public ArmRpoExecutionDetailEntity minimalArmRpoExecutionDetailEntity() {
        return someMinimal();
    }

    @Override
    public ArmRpoExecutionDetailEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilderRetrieve retrieve = new
            TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilderRetrieve();

        retrieve.getBuilder()
            .lastModifiedById(0)
            .createdById(0)
            .createdDateTime(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now());
        return retrieve;
    }
}
