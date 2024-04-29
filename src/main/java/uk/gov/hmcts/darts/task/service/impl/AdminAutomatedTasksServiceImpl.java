package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAutomatedTasksServiceImpl implements AdminAutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;

    @Override
    public List<AutomatedTaskSummary> getAllAutomatedTasks() {
        var automatedTask = automatedTaskRepository.findAll();
        return mapEntitiesToModel(automatedTask);
    }

    @Override
    public DetailedAutomatedTask getAutomatedTaskById(Integer taskId) {
        var maybeAutomatedTask = automatedTaskRepository.findById(taskId);
        if (maybeAutomatedTask.isEmpty()) {
            throw new DartsApiException(AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
        }

        return mapEntityToDetailedAutomatedTask(maybeAutomatedTask.get());
    }

    private DetailedAutomatedTask mapEntityToDetailedAutomatedTask(AutomatedTaskEntity automatedTaskEntity) {
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
            .isCronEditable(automatedTaskEntity.getCronEditable());
    }

    private List<AutomatedTaskSummary> mapEntitiesToModel(List<AutomatedTaskEntity> automatedTaskEntities) {
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
