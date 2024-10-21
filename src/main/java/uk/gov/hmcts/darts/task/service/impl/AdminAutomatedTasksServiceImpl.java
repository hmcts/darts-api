package uk.gov.hmcts.darts.task.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.ENABLE_DISABLE_JOB;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.RUN_JOB_MANUALLY;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_ALREADY_RUNNING;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class AdminAutomatedTasksServiceImpl implements AdminAutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;
    private final AutomatedTasksMapper mapper;
    private final ManualTaskService manualTaskService;
    private final AutomatedTaskRunner automatedTaskRunner;
    private final AuditApi auditApi;
    private final LockService lockService;

    private final ConfigurableBeanFactory configurableBeanFactory;

    @Override
    public List<AutomatedTaskSummary> getAllAutomatedTasks() {
        var automatedTask = automatedTaskRepository.findAll()
            .stream()
            .filter(this::shouldIncludeAutomatedTask)
            .toList();
        return mapper.mapEntitiesToModel(automatedTask);
    }

    @Override
    public DetailedAutomatedTask getAutomatedTaskById(Integer taskId) {
        return mapper.mapEntityToDetailedAutomatedTask(getAutomatedTaskEntityById(taskId));
    }


    @Override
    public void runAutomatedTask(Integer taskId) {
        var automatedTaskEntity = getAutomatedTaskEntityById(taskId);

        if (lockService.isLocked(automatedTaskEntity)) {
            log.info("Manual running of {} failed as it is locked", automatedTaskEntity.getTaskName());
            throw new DartsApiException(AUTOMATED_TASK_ALREADY_RUNNING);
        }

        var automatedTask = manualTaskService.getAutomatedTasks().stream()
            .filter(task -> task.getTaskName().equals(automatedTaskEntity.getTaskName()))
            .findFirst();

        if (automatedTask.isEmpty()) {
            log.error("Manual running of {} failed as it does not exist", automatedTaskEntity.getTaskName());
            throw new DartsApiException(AutomatedTaskApiError.AUTOMATED_TASK_NOT_CONFIGURED_CORRECTLY);
        }

        automatedTaskRunner.run(automatedTask.get());

        auditApi.record(RUN_JOB_MANUALLY);
    }

    @Override
    public DetailedAutomatedTask updateAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch) {
        var automatedTask = getAutomatedTaskEntityById(taskId);

        if (automatedTaskPatch.getIsActive() != null) {
            automatedTask.setTaskEnabled(automatedTaskPatch.getIsActive());
            auditApi.record(ENABLE_DISABLE_JOB);
        }

        if (automatedTaskPatch.getBatchSize() != null) {
            automatedTask.setBatchSize(automatedTaskPatch.getBatchSize());
            log.info("Batch size for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getBatchSize());
        }

        var updatedTask = automatedTaskRepository.save(automatedTask);

        return mapper.mapEntityToDetailedAutomatedTask(updatedTask);
    }

    private boolean shouldIncludeAutomatedTask(AutomatedTaskEntity automatedTaskEntity) {
        AutomatedTaskName automatedTaskName = AutomatedTaskName.valueOfTaskName(automatedTaskEntity.getTaskName());
        if (automatedTaskName == null || automatedTaskName.getConditionalOnSpEL() == null) {
            return true;
        }
        String embeddedValue = configurableBeanFactory.resolveEmbeddedValue(automatedTaskName.getConditionalOnSpEL());
        assert embeddedValue != null;
        return Boolean.TRUE.equals(new SpelExpressionParser().parseExpression(embeddedValue).getValue(Boolean.class));
    }

    private AutomatedTaskEntity getAutomatedTaskEntityById(Integer taskId) {
        var maybeAutomatedTask = automatedTaskRepository.findById(taskId);
        if (maybeAutomatedTask.isEmpty() || !shouldIncludeAutomatedTask(maybeAutomatedTask.get())) {
            throw new DartsApiException(AUTOMATED_TASK_NOT_FOUND);
        }
        return maybeAutomatedTask.get();
    }
}