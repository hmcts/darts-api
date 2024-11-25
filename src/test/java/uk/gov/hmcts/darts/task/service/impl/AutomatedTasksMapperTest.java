package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AutomatedTasksMapperTest {

    private AutomatedTasksMapper automatedTasksMapper;

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2023, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LAST_MODIFIED_AT = OffsetDateTime.of(2023, 10, 2, 12, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        automatedTasksMapper = new AutomatedTasksMapper();
    }

    @Test
    void shouldMapEntityToDetailedAutomatedTaskWithoutArmTask() {
        // Given
        UserAccountEntity createdBy = new UserAccountEntity();
        createdBy.setId(1);

        UserAccountEntity lastModifiedBy = new UserAccountEntity();
        lastModifiedBy.setId(2);

        AutomatedTaskEntity entity = createBaseAutomatedTaskEntity();

        // When
        DetailedAutomatedTask result = automatedTasksMapper.mapEntityToDetailedAutomatedTask(entity);

        // Then
        assertCommonFields(result);
        assertThat(result.getRpoCsvEndHour()).isNull();
        assertThat(result.getRpoCsvStartHour()).isNull();
        assertThat(result.getArmReplayStartTs()).isNull();
        assertThat(result.getArmReplayEndTs()).isNull();
        assertThat(result.getArmAttributeType()).isNull();
    }

    @Test
    void shouldMapEntityToDetailedAutomatedTaskWithArmTask() {
        // Given
        UserAccountEntity createdBy = new UserAccountEntity();
        createdBy.setId(1);

        UserAccountEntity lastModifiedBy = new UserAccountEntity();
        lastModifiedBy.setId(2);

        AutomatedTaskEntity entity = createBaseAutomatedTaskEntity();

        ArmAutomatedTaskEntity armAutomatedTaskEntity = new ArmAutomatedTaskEntity();
        armAutomatedTaskEntity.setRpoCsvStartHour(1);
        armAutomatedTaskEntity.setRpoCsvEndHour(2);
        armAutomatedTaskEntity.setArmReplayStartTs(OffsetDateTime.of(2023, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        armAutomatedTaskEntity.setArmReplayEndTs(OffsetDateTime.of(2023, 10, 2, 12, 0, 0, 0, ZoneOffset.UTC));
        armAutomatedTaskEntity.setArmAttributeType("test");
        entity.setArmAutomatedTaskEntity(armAutomatedTaskEntity);

        // When
        DetailedAutomatedTask result = automatedTasksMapper.mapEntityToDetailedAutomatedTask(entity);

        // Then
        assertCommonFields(result);
        assertThat(result.getRpoCsvEndHour()).isEqualTo(2);
        assertThat(result.getRpoCsvStartHour()).isEqualTo(1);
        assertThat(result.getArmReplayStartTs()).isEqualTo(OffsetDateTime.of(2023, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        assertThat(result.getArmReplayEndTs()).isEqualTo(OffsetDateTime.of(2023, 10, 2, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(result.getArmAttributeType()).isEqualTo("test");

    }

    private AutomatedTaskEntity createBaseAutomatedTaskEntity() {

        UserAccountEntity createdBy = new UserAccountEntity();
        createdBy.setId(1);

        UserAccountEntity lastModifiedBy = new UserAccountEntity();
        lastModifiedBy.setId(2);

        AutomatedTaskEntity entity = new AutomatedTaskEntity();
        entity.setId(123);
        entity.setTaskName("Test Task");
        entity.setTaskEnabled(true);
        entity.setTaskDescription("A test task for demonstration");
        entity.setCronExpression("0 0 * * *");
        entity.setCreatedDateTime(CREATED_AT);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedDateTime(LAST_MODIFIED_AT);
        entity.setLastModifiedBy(lastModifiedBy);
        entity.setCronEditable(true);
        entity.setBatchSize(50);

        return entity;
    }

    private void assertCommonFields(DetailedAutomatedTask result) {

        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Task");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getDescription()).isEqualTo("A test task for demonstration");
        assertThat(result.getCronExpression()).isEqualTo("0 0 * * *");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(result.getCreatedBy()).isEqualTo(1);
        assertThat(result.getLastModifiedAt()).isEqualTo(LAST_MODIFIED_AT);
        assertThat(result.getLastModifiedBy()).isEqualTo(2);
        assertThat(result.getIsCronEditable()).isTrue();
        assertThat(result.getBatchSize()).isEqualTo(50);
    }
}
