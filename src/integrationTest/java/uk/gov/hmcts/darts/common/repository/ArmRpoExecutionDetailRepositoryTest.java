package uk.gov.hmcts.darts.common.repository;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum.GET_PRODUCTION_OUTPUT_FILES;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum.REMOVE_PRODUCTION;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum.COMPLETED;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum.FAILED;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum.IN_PROGRESS;
import static uk.gov.hmcts.darts.test.common.data.ArmRpoStateEntityTestData.stateOf;
import static uk.gov.hmcts.darts.test.common.data.ArmRpoStatusEntityTestData.statusOf;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

class ArmRpoExecutionDetailRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity2;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity3;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity4;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity5;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity6;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity7;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity8;
    
    private static final String PRODUCTION_ID = "some-production-id";


    @BeforeEach
    public void beforeAll() {
        armRpoExecutionDetailEntity1 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, REMOVE_PRODUCTION, PRODUCTION_ID));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY, PRODUCTION_ID));
        armRpoExecutionDetailEntity2 = dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY, 
                                                                                               PRODUCTION_ID));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY, PRODUCTION_ID));
        armRpoExecutionDetailEntity3 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY, 
                                                                                               PRODUCTION_ID));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, SAVE_BACKGROUND_SEARCH, PRODUCTION_ID));
        armRpoExecutionDetailEntity4 = dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, SAVE_BACKGROUND_SEARCH, PRODUCTION_ID));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, SAVE_BACKGROUND_SEARCH, PRODUCTION_ID));
        armRpoExecutionDetailEntity5 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, SAVE_BACKGROUND_SEARCH, PRODUCTION_ID));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_PRODUCTION_OUTPUT_FILES, PRODUCTION_ID));
        armRpoExecutionDetailEntity6 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_PRODUCTION_OUTPUT_FILES, PRODUCTION_ID));
        armRpoExecutionDetailEntity7 = dartsPersistence.save(createArmRpoExecutionDetailEntity(IN_PROGRESS, SAVE_BACKGROUND_SEARCH, PRODUCTION_ID));
       armRpoExecutionDetailEntity8 = dartsPersistence.save(createArmRpoExecutionDetailEntity(IN_PROGRESS, SAVE_BACKGROUND_SEARCH, null));

        // update automatically set lastModifiedDateTime for test purposes. This defaults to now() on save
        updateLastModifiedToDaysAgo(armRpoExecutionDetailEntity7, 10);
        updateLastModifiedToDaysAgo(armRpoExecutionDetailEntity8, 10);
        
    }

    @Test
    void findLatestByCreatedDateTimeDescShouldReturnLatestArmRpoExecutionDetail() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDesc();

            // then
            assertThat(result.get()).isEqualTo(armRpoExecutionDetailEntity8);
        });
    }

    @Test
    void findLatestByCreatedDateTimeDescWithStateAndStatusShouldReturnLatestArmRpoExecutionDetail() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result1 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(REMOVE_PRODUCTION), statusOf(COMPLETED));
            var result2 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY), statusOf(FAILED));
            var result3 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY), statusOf(COMPLETED));
            var result4 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(SAVE_BACKGROUND_SEARCH), statusOf(FAILED));
            var result5 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(SAVE_BACKGROUND_SEARCH), statusOf(COMPLETED));
            var result6 = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDescWithStateAndStatus(
                stateOf(GET_PRODUCTION_OUTPUT_FILES), statusOf(COMPLETED));

            // then
            assertThat(result1.get()).isEqualTo(armRpoExecutionDetailEntity1);
            assertThat(result2.get()).isEqualTo(armRpoExecutionDetailEntity2);
            assertThat(result3.get()).isEqualTo(armRpoExecutionDetailEntity3);
            assertThat(result4.get()).isEqualTo(armRpoExecutionDetailEntity4);
            assertThat(result5.get()).isEqualTo(armRpoExecutionDetailEntity5);
            assertThat(result6.get()).isEqualTo(armRpoExecutionDetailEntity6);
        });

    }
    
    @Test
    void findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter_ShouldReturnIds_WhenEntitiesMatchCriteria() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter(
                statusOf(IN_PROGRESS),
                OffsetDateTime.now().minusDays(5)
            );

            // then
            assertThat(result).contains(armRpoExecutionDetailEntity7.getId());
            assertThat(result.size()).isEqualTo(1);
        });
    }

    @Test
    void findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter_ShouldReturnNoIds_WhenEntitiesDontMatchCutOffTIme() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter(
                statusOf(COMPLETED),
                OffsetDateTime.now().minusDays(11)
            );

            // then
            assertThat(result.size()).isEqualTo(0);
        });
    }

    @Test
    void findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter_ShouldReturnNoIds_WhenEntitiesDontMatchProductionId() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findIdsByStatusWithProductionIdAndLastModifiedDateTimeAfter(
                statusOf(COMPLETED),
                OffsetDateTime.now().minusDays(5)
            );

            // then
            assertThat(result.size()).isEqualTo(0);
        });
    }

    @Test
    void updateLastModifiedDateTimeById_ShouldUpdateLastModifiedDateTime_WhenIdExists() {
        transactionalUtil.executeInTransaction(() -> {
            // given
            var newLastModifiedDateTime = OffsetDateTime.now().minusDays(20);

            // when
            armRpoExecutionDetailRepository.updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity7.getId(),
                newLastModifiedDateTime.truncatedTo(ChronoUnit.MICROS)
            );

            // then
            var updatedEntity = armRpoExecutionDetailRepository.findById(armRpoExecutionDetailEntity7.getId());
            var expected = newLastModifiedDateTime.truncatedTo(ChronoUnit.MICROS); 
            assertThat(updatedEntity.isPresent()).isTrue();
            assertThat(updatedEntity.get().getLastModifiedDateTime()).isEqualTo(expected);
        });
    }

    private static @NotNull ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(ArmRpoStatusEnum status, ArmRpoStateEnum state, String productionId) {
        var armRpoExecutionDetailEntity = getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setArmRpoStatus(statusOf(status));
        armRpoExecutionDetailEntity.setArmRpoState(stateOf(state));
        armRpoExecutionDetailEntity.setProductionId(productionId);
        return armRpoExecutionDetailEntity;
    }

    private void updateLastModifiedToDaysAgo(ArmRpoExecutionDetailEntity executionDetail, long daysAgo) {
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                executionDetail.getId(),
                OffsetDateTime.now().minusDays(daysAgo)
            );
    }

}
