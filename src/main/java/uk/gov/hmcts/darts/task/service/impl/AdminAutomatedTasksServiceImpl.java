package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTask;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAutomatedTasksServiceImpl implements AdminAutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;

    @Override
    public List<AutomatedTask> getAllAutomatedTasks() {
        var automatedTask = automatedTaskRepository.findAll();
        return mapEntitiesToModel(automatedTask);
    }

    private List<AutomatedTask> mapEntitiesToModel(List<AutomatedTaskEntity> automatedTaskEntities) {
        return automatedTaskEntities.stream()
            .map(this::mapEntityToModel)
            .toList();
    }

    private AutomatedTask mapEntityToModel(AutomatedTaskEntity automatedTaskEntity) {
        return new AutomatedTask()
            .id(automatedTaskEntity.getId())
            .name(automatedTaskEntity.getTaskName())
            .isActive(automatedTaskEntity.getTaskEnabled())
            .description(automatedTaskEntity.getTaskDescription())
            .cronExpression(automatedTaskEntity.getCronExpression());
    }
}
