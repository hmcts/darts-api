package uk.gov.hmcts.darts.task.service.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

@Component
public class AutomatedTasksMapper {

    public DetailedAutomatedTask mapEntityToDetailedAutomatedTask(AutomatedTaskEntity automatedTaskEntity) {
        return new DetailedAutomatedTask()
            .id(automatedTaskEntity.getId())
            .name(automatedTaskEntity.getTaskName())
            .isActive(automatedTaskEntity.getTaskEnabled())
            .description(automatedTaskEntity.getTaskDescription())
            .cronExpression(automatedTaskEntity.getCronExpression())
            .createdAt(automatedTaskEntity.getCreatedDateTime())
            .createdBy(automatedTaskEntity.getCreatedBy().getId())
            .lastModifiedAt(automatedTaskEntity.getLastModifiedDateTime())
            .lastModifiedBy(automatedTaskEntity.getLastModifiedBy().getId())
            .isCronEditable(automatedTaskEntity.getCronEditable())
            .batchSize(automatedTaskEntity.getBatchSize());
    }

    public List<AutomatedTaskSummary> mapEntitiesToModel(List<AutomatedTaskEntity> automatedTaskEntities) {
        return automatedTaskEntities.stream()
            .map(this::mapEntityToTaskSummary)
            .toList();
    }

    private AutomatedTaskSummary mapEntityToTaskSummary(AutomatedTaskEntity automatedTaskEntity) {
        return new AutomatedTaskSummary()
            .id(automatedTaskEntity.getId())
            .name(automatedTaskEntity.getTaskName())
            .isActive(automatedTaskEntity.getTaskEnabled())
            .description(automatedTaskEntity.getTaskDescription())
            .cronExpression(automatedTaskEntity.getCronExpression());
    }
}
