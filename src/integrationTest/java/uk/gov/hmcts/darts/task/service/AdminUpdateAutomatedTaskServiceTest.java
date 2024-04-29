package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.ExcessiveImports")
class AdminUpdateAutomatedTaskServiceTest extends IntegrationBase {

    @Autowired
    private AdminAutomatedTaskService adminAutomatedTaskService;

    @Test
    void updatesIsEnabledFieldFalse() {
        var persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        final boolean initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(true);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        var response = adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(false));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isFalse();

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

        // clean up
        persistedAutomatedTaskEntity.setTaskEnabled(initialValue);
        dartsDatabase.save(persistedAutomatedTaskEntity);
    }

    @Test
    void updatesIsEnabledFieldTrue() {
        var persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        final boolean initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(false);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        var resposne = adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(true));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isTrue();

        assertThat(resposne.getId()).isEqualTo(1);
        assertThat(resposne.getName()).isEqualTo(updatedAutomatedTaskEntity.getTaskName());
        assertThat(resposne.getDescription()).isEqualTo(updatedAutomatedTaskEntity.getTaskDescription());
        assertThat(resposne.getCronExpression()).isEqualTo(updatedAutomatedTaskEntity.getCronExpression());
        assertThat(resposne.getIsActive()).isEqualTo(updatedAutomatedTaskEntity.getTaskEnabled());
        assertThat(resposne.getIsCronEditable()).isEqualTo(updatedAutomatedTaskEntity.getCronEditable());
        assertThat(resposne.getCreatedAt()).isEqualTo(updatedAutomatedTaskEntity.getCreatedDateTime());
        assertThat(resposne.getCreatedBy()).isEqualTo(updatedAutomatedTaskEntity.getCreatedBy().getId());
        assertThat(resposne.getLastModifiedAt()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedDateTime());
        assertThat(resposne.getLastModifiedBy()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedBy().getId());

        // clean up
        persistedAutomatedTaskEntity.setTaskEnabled(initialValue);
        dartsDatabase.save(persistedAutomatedTaskEntity);
    }
}