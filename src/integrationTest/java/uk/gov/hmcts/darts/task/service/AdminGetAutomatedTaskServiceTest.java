package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.ExcessiveImports")
class AdminGetAutomatedTaskServiceTest extends IntegrationBase {

    @Autowired
    private AdminAutomatedTaskService adminAutomatedTaskService;

    @Test
    void findsAutomatedTasksById() {
        var automatedTask = dartsDatabase.getAutomatedTask(1);

        var automatedTasks = adminAutomatedTaskService.getAutomatedTaskById(1);

        assertThat(automatedTasks.getId()).isEqualTo(1);
        assertThat(automatedTasks.getName()).isEqualTo(automatedTask.getTaskName());
        assertThat(automatedTasks.getDescription()).isEqualTo(automatedTask.getTaskDescription());
        assertThat(automatedTasks.getCronExpression()).isEqualTo(automatedTask.getCronExpression());
        assertThat(automatedTasks.getIsActive()).isEqualTo(automatedTask.getTaskEnabled());
        assertThat(automatedTasks.getIsCronEditable()).isEqualTo(automatedTask.getCronEditable());
        assertThat(automatedTasks.getCreatedAt()).isEqualTo(automatedTask.getCreatedDateTime());
        assertThat(automatedTasks.getCreatedBy()).isEqualTo(automatedTask.getCreatedById());
        assertThat(automatedTasks.getLastModifiedAt()).isEqualTo(automatedTask.getLastModifiedDateTime());
        assertThat(automatedTasks.getLastModifiedBy()).isEqualTo(automatedTask.getLastModifiedById());
        assertThat(automatedTasks.getBatchSize()).isEqualTo(automatedTask.getBatchSize());
    }

    @Test
    void findsAllAutomatedTasks() {
        var persistedTasks = dartsDatabase.getAllAutomatedTasks();
        //Remove tasks that are feature flagged off
        persistedTasks.removeIf(automatedTaskEntity -> automatedTaskEntity.getTaskName().equals(AutomatedTaskName.PROCESS_ARM_RPO_PENDING.getTaskName()));
        var automatedTasks = adminAutomatedTaskService.getAllAutomatedTasksSummaries();

        assertThat(automatedTasks).extracting("id").isEqualTo(taskIdsOf(persistedTasks));
        assertThat(automatedTasks).extracting("name").isEqualTo(taskNamesOf(persistedTasks));
        assertThat(automatedTasks).extracting("description").isEqualTo(taskDescriptionsOf(persistedTasks));
        assertThat(automatedTasks).extracting("cronExpression").isEqualTo(cronExpressionsOf(persistedTasks));
        assertThat(automatedTasks).extracting("isActive").isEqualTo(taskIsActiveOf(persistedTasks));
    }

    private List<Boolean> taskIsActiveOf(List<AutomatedTaskEntity> persistedTasks) {
        return persistedTasks.stream()
            .sorted((task1, task2) -> task1.getTaskName().compareTo(task2.getTaskName()))
            .map(AutomatedTaskEntity::getTaskEnabled)
            .toList();
    }

    private List<String> cronExpressionsOf(List<AutomatedTaskEntity> persistedTasks) {
        return persistedTasks.stream()
            .sorted((task1, task2) -> task1.getTaskName().compareTo(task2.getTaskName()))
            .map(AutomatedTaskEntity::getCronExpression)
            .toList();
    }

    private List<String> taskDescriptionsOf(List<AutomatedTaskEntity> automatedTask) {
        return automatedTask.stream()
            .sorted((task1, task2) -> task1.getTaskName().compareTo(task2.getTaskName()))
            .map(AutomatedTaskEntity::getTaskDescription)
            .toList();
    }

    private List<String> taskNamesOf(List<AutomatedTaskEntity> automatedTask) {
        return automatedTask.stream()
            .sorted((task1, task2) -> task1.getTaskName().compareTo(task2.getTaskName()))
            .map(AutomatedTaskEntity::getTaskName)
            .toList();
    }

    private List<Integer> taskIdsOf(List<AutomatedTaskEntity> automatedTask) {
        return automatedTask.stream()
            .sorted((task1, task2) -> task1.getTaskName().compareTo(task2.getTaskName()))
            .map(AutomatedTaskEntity::getId)
            .toList();
    }
}