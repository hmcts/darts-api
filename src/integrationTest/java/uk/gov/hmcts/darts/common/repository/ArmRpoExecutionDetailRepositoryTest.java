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


    @BeforeEach
    public void beforeAll() {
        armRpoExecutionDetailEntity1 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, REMOVE_PRODUCTION));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY));
        armRpoExecutionDetailEntity2 = dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY));
        armRpoExecutionDetailEntity3 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, SAVE_BACKGROUND_SEARCH));
        armRpoExecutionDetailEntity4 = dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, SAVE_BACKGROUND_SEARCH));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, SAVE_BACKGROUND_SEARCH));
        armRpoExecutionDetailEntity5 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, SAVE_BACKGROUND_SEARCH));
        dartsPersistence.save(createArmRpoExecutionDetailEntity(FAILED, GET_PRODUCTION_OUTPUT_FILES));
        armRpoExecutionDetailEntity6 = dartsPersistence.save(createArmRpoExecutionDetailEntity(COMPLETED, GET_PRODUCTION_OUTPUT_FILES));
        armRpoExecutionDetailEntity7 = dartsPersistence.save(createArmRpoExecutionDetailEntity(IN_PROGRESS, SAVE_BACKGROUND_SEARCH));

        // update automatically set lastModifiedDateTime for test purposes. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity7.getId(),
                OffsetDateTime.now().minusDays(10)
            );
        
    }

    @Test
    void findLatestByCreatedDateTimeDescShouldReturnLatestArmRpoExecutionDetail() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findLatestByCreatedDateTimeDesc();

            // then
            assertThat(result.get()).isEqualTo(armRpoExecutionDetailEntity7);
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
    void findIdsByStatusAndLastModifiedDateTimeAfter_ShouldReturnIds_WhenEntitiesMatchCriteria() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findIdsByStatusAndLastModifiedDateTimeAfter(
                statusOf(IN_PROGRESS),
                OffsetDateTime.now().minusDays(5)
            );

            // then
            assertThat(result).contains(armRpoExecutionDetailEntity7.getId());
            assertThat(result.size()).isEqualTo(1);
        });
    }

    @Test
    void findIdsByStatusAndLastModifiedDateTimeAfter_ShouldReturnNoIds_WhenEntitiesDontMatchCriteria() {
        transactionalUtil.executeInTransaction(() -> {
            // when
            var result = armRpoExecutionDetailRepository.findIdsByStatusAndLastModifiedDateTimeAfter(
                statusOf(COMPLETED),
                OffsetDateTime.now().minusDays(11)
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
                newLastModifiedDateTime
            );

            // then
            var updatedEntity = armRpoExecutionDetailRepository.findById(armRpoExecutionDetailEntity7.getId());
            assertThat(updatedEntity.isPresent()).isTrue();
            assertThat(updatedEntity.get().getLastModifiedDateTime()).isEqualTo(newLastModifiedDateTime);
        });
    }

    private static @NotNull ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(ArmRpoStatusEnum status, ArmRpoStateEnum state) {
        var armRpoExecutionDetailEntity = getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setArmRpoStatus(statusOf(status));
        armRpoExecutionDetailEntity.setArmRpoState(stateOf(state));
        return armRpoExecutionDetailEntity;
    }

}
