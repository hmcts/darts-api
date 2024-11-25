package uk.gov.hmcts.darts.task.service.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

@Component
public class AutomatedTasksMapper {

    public DetailedAutomatedTask mapEntityToDetailedAutomatedTask(AutomatedTaskEntity automatedTaskEntity) {
        DetailedAutomatedTask detailedAutomatedTask = new DetailedAutomatedTask()
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

        ArmAutomatedTaskEntity armAutomatedTaskEntity = automatedTaskEntity.getArmAutomatedTaskEntity();

        if (armAutomatedTaskEntity != null) {
            detailedAutomatedTask.setRpoCsvStartHour(armAutomatedTaskEntity.getRpoCsvStartHour());
            detailedAutomatedTask.setRpoCsvEndHour(armAutomatedTaskEntity.getRpoCsvEndHour());
            detailedAutomatedTask.setArmReplayStartTs(armAutomatedTaskEntity.getArmReplayStartTs());
            detailedAutomatedTask.setArmReplayEndTs(armAutomatedTaskEntity.getArmReplayEndTs());
            detailedAutomatedTask.setArmAttributeType(armAutomatedTaskEntity.getArmAttributeType());
        }
        return detailedAutomatedTask;
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
