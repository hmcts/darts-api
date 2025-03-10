package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestArmRpoExecutionDetailEntity extends ArmRpoExecutionDetailEntity implements DbInsertable<ArmRpoExecutionDetailEntity> {

    @lombok.Builder
    public TestArmRpoExecutionDetailEntity(Integer id, ArmRpoStateEntity armRpoState, ArmRpoStatusEntity armRpoStatus,
                                           String matterId, String indexId, String entitlementId, String storageAccountId,
                                           String searchId, String productionId, String sortingField, Integer searchItemCount,
                                           UserAccountEntity createdBy, UserAccountEntity lastModifiedBy,
                                           OffsetDateTime createdDateTime, OffsetDateTime lastModifiedDateTime) {
        setId(id);
        setArmRpoState(armRpoState);
        setArmRpoStatus(armRpoStatus);
        setMatterId(matterId);
        setIndexId(indexId);
        setEntitlementId(entitlementId);
        setStorageAccountId(storageAccountId);
        setSearchId(searchId);
        setProductionId(productionId);
        setSortingField(sortingField);
        setSearchItemCount(searchItemCount);
        setCreatedBy(createdBy);
        setLastModifiedBy(lastModifiedBy);
        setCreatedDateTime(createdDateTime);
        setLastModifiedDateTime(lastModifiedDateTime);
    }

    @Override
    public ArmRpoExecutionDetailEntity getEntity() {
        try {
            ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
            BeanUtils.copyProperties(armRpoExecutionDetailEntity, this);
            return armRpoExecutionDetailEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestArmRpoExecutionDetailEntityBuilderRetrieve
        implements BuilderHolder<TestArmRpoExecutionDetailEntity, TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilder> {

        private TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilder builder = TestArmRpoExecutionDetailEntity.builder();

        @Override
        public TestArmRpoExecutionDetailEntity build() {
            return builder.build();
        }

        @Override
        public TestArmRpoExecutionDetailEntity.TestArmRpoExecutionDetailEntityBuilder getBuilder() {
            return builder;
        }
    }
}
