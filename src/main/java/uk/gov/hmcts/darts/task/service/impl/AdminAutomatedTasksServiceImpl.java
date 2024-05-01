package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    private final AutomatedTasksMapper mapper;

    @Override
    public List<AutomatedTaskSummary> getAllAutomatedTasks() {
        var automatedTask = automatedTaskRepository.findAll();
        return mapper.mapEntitiesToModel(automatedTask);
    }

    @Override
    public DetailedAutomatedTask getAutomatedTaskById(Integer taskId) {
        var maybeAutomatedTask = automatedTaskRepository.findById(taskId);
        if (maybeAutomatedTask.isEmpty()) {
            throw new DartsApiException(AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
        }

        return mapper.mapEntityToDetailedAutomatedTask(maybeAutomatedTask.get());
    }
}
