package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_ALREADY_RUNNING;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAutomatedTasksServiceImpl implements AdminAutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;
    private final AutomatedTasksMapper mapper;
    private final AutomatedTaskService automatedTaskService;
    private final AutomatedTaskRunner automatedTaskRunner;
    private final CurrentTimeHelper currentTimeHelper;

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

    @Override
    public void runAutomatedTask(Integer taskId) {
        var automatedTaskEntity = automatedTaskRepository.findById(taskId)
            .orElseThrow(() -> new DartsApiException(AUTOMATED_TASK_NOT_FOUND));

        if (isLocked(automatedTaskEntity)) {
            log.info("Manual running of {} failed as it is locked", automatedTaskEntity.getTaskName());
            throw new DartsApiException(AUTOMATED_TASK_ALREADY_RUNNING);
        }

        var automatedTask = automatedTaskService.getAutomatedTasks().stream()
            .filter(task -> task.getTaskName().equals(automatedTaskEntity.getTaskName()))
            .findFirst();

        if (automatedTask.isEmpty()) {
            log.error("Manual running of {} failed as it does not exist", automatedTaskEntity.getTaskName());
            throw new DartsApiException(AutomatedTaskApiError.AUTOMATED_TASK_NOT_CONFIGURED_CORRECTLY);
        }

        automatedTaskRunner.run(automatedTask.get());
    }

    @Override
    public DetailedAutomatedTask updateAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch) {
        var automatedTask = automatedTaskRepository.findById(taskId)
            .orElseThrow(() -> new DartsApiException(AUTOMATED_TASK_NOT_FOUND));

        automatedTask.setTaskEnabled(automatedTaskPatch.getIsActive());

        var updatedTask = automatedTaskRepository.save(automatedTask);

        return mapper.mapEntityToDetailedAutomatedTask(updatedTask);
    }

    private boolean isLocked(AutomatedTaskEntity automatedTask) {
        var lockedUntil = automatedTaskRepository.findLockedUntilForTask(automatedTask.getTaskName());
        return !lockedUntil.isEmpty() && isInFuture(lockedUntil);
    }

    private boolean isInFuture(List<Timestamp> lockedUntil) {
        // There should only ever be one item in the list as we search by primary key
        return toOffsetDateTime(lockedUntil.get(0)).isAfter(currentTimeHelper.currentOffsetDateTime());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
    }

}
