package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.ExcessiveImports")
class AdminUpdateAutomatedTaskServiceTest extends IntegrationBase {

    @Autowired
    private AdminAutomatedTaskService adminAutomatedTaskService;
    private boolean initialValue;
    private AutomatedTaskEntity persistedAutomatedTaskEntity;

    @AfterEach
    void tearDown() {
        persistedAutomatedTaskEntity.setTaskEnabled(initialValue);
        dartsDatabase.save(persistedAutomatedTaskEntity);
    }

    @Test
    void canUpdateIsEnabledFieldToFalse() {
        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(true);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(false));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isFalse();
    }

    @Test
    void canUpdateIsEnabledFieldToTrue() {
        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(false);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(true));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isTrue();
    }

    @Test
    void returnsDetailedAutomatedTaskAfterUpdate() {
        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up

        var response = adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(true));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo(updatedAutomatedTaskEntity.getTaskName());
        assertThat(response.getDescription()).isEqualTo(updatedAutomatedTaskEntity.getTaskDescription());
        assertThat(response.getCronExpression()).isEqualTo(updatedAutomatedTaskEntity.getCronExpression());
        assertThat(response.getIsActive()).isEqualTo(updatedAutomatedTaskEntity.getTaskEnabled());
        assertThat(response.getIsCronEditable()).isEqualTo(updatedAutomatedTaskEntity.getCronEditable());
        assertThat(response.getCreatedAt()).isEqualTo(updatedAutomatedTaskEntity.getCreatedDateTime());
        assertThat(response.getCreatedBy()).isEqualTo(updatedAutomatedTaskEntity.getCreatedBy().getId());
        assertThat(response.getLastModifiedAt()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedDateTime());
        assertThat(response.getLastModifiedBy()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedBy().getId());
    }
}