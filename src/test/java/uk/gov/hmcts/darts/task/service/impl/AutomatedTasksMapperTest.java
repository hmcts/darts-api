package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AutomatedTasksMapperTest {

    private AutomatedTasksMapper automatedTasksMapper;

    @BeforeEach
    void setUp() {
        automatedTasksMapper = new AutomatedTasksMapper();
    }

    @Test
    void shouldMapEntityToDetailedAutomatedTask() {
        // Given
        UserAccountEntity createdBy = new UserAccountEntity();
        createdBy.setId(1);

        UserAccountEntity lastModifiedBy = new UserAccountEntity();
        lastModifiedBy.setId(2);

        OffsetDateTime createdAt = OffsetDateTime.of(2023, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lastModifiedAt = OffsetDateTime.of(2023, 10, 2, 12, 0, 0, 0, ZoneOffset.UTC);

        AutomatedTaskEntity entity = new AutomatedTaskEntity();
        entity.setId(123);
        entity.setTaskName("Test Task");
        entity.setTaskEnabled(true);
        entity.setTaskDescription("A test task for demonstration");
        entity.setCronExpression("0 0 * * *");
        entity.setCreatedDateTime(createdAt);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedDateTime(lastModifiedAt);
        entity.setLastModifiedBy(lastModifiedBy);
        entity.setCronEditable(true);
        entity.setBatchSize(50);

        // When
        DetailedAutomatedTask result = automatedTasksMapper.mapEntityToDetailedAutomatedTask(entity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Task");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getDescription()).isEqualTo("A test task for demonstration");
        assertThat(result.getCronExpression()).isEqualTo("0 0 * * *");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getCreatedBy()).isEqualTo(1);
        assertThat(result.getLastModifiedAt()).isEqualTo(lastModifiedAt);
        assertThat(result.getLastModifiedBy()).isEqualTo(2);
        assertThat(result.getIsCronEditable()).isTrue();
        assertThat(result.getBatchSize()).isEqualTo(50);
    }
}
