package uk.gov.hmcts.darts.task.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.ENABLE_DISABLE_JOB;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.RUN_JOB_MANUALLY;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_ALREADY_RUNNING;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.INCORRECT_AUTOMATED_TASK_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class AdminAutomatedTasksServiceImpl implements AdminAutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final AutomatedTasksMapper mapper;
    private final AutomatedTaskRunner automatedTaskRunner;
    private final AuditApi auditApi;
    private final LockService lockService;
    private final ConfigurableBeanFactory configurableBeanFactory;
    private final List<AutomatedTask> automatedTasks;

    @Override
    public List<AutomatedTaskSummary> getAllAutomatedTasksSummaries() {
        return mapper.mapEntitiesToModel(getAllAutomatedTasksEntities());
    }

    List<AutomatedTaskEntity> getAllAutomatedTasksEntities() {
        return automatedTaskRepository.findAll()
            .stream()
            .filter(this::shouldIncludeAutomatedTask)
            .toList();
    }

    List<AutomatedTask> getAllAutomatedTasks() {
        return automatedTasks;
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

        var automatedTask = getAllAutomatedTasks()
            .stream()
            .filter(task -> task.getTaskName().equals(automatedTaskEntity.getTaskName()))
            .findFirst();

        if (automatedTask.isEmpty()) {
            log.error("Manual running of {} failed as it does not exist", automatedTaskEntity.getTaskName());
            throw new DartsApiException(AutomatedTaskApiError.AUTOMATED_TASK_NOT_CONFIGURED_CORRECTLY);
        }

        automatedTaskRunner.run(automatedTask.get(), true);

        auditApi.record(RUN_JOB_MANUALLY, automatedTaskEntity.getTaskName());
    }

    @Override
    @Transactional
    public DetailedAutomatedTask updateAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch) {
        var automatedTask = getAutomatedTaskEntityById(taskId);

        if (automatedTaskPatch.getIsActive() != null) {
            automatedTask.setTaskEnabled(automatedTaskPatch.getIsActive());
            auditApi.record(ENABLE_DISABLE_JOB, automatedTask.getTaskName() + " " + (automatedTaskPatch.getIsActive() ? "enabled" : "disabled"));
            log.info("Task {} is now {}", automatedTask.getTaskName(), automatedTaskPatch.getIsActive() ? "enabled" : "disabled");
        }

        if (automatedTaskPatch.getBatchSize() != null) {
            automatedTask.setBatchSize(automatedTaskPatch.getBatchSize());
            log.info("Batch size for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getBatchSize());
        }

        //Arm Autoamted Task updates
        List<Consumer<ArmAutomatedTaskEntity>> armAutomatedTaskEntityConsumer = new ArrayList<>();

        if (automatedTaskPatch.getArmReplayStartTs() != null) {
            armAutomatedTaskEntityConsumer.add(armAutomatedTaskEntity -> {
                armAutomatedTaskEntity.setArmReplayStartTs(automatedTaskPatch.getArmReplayStartTs());
                log.info("ARM replay start timestamp for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getArmReplayStartTs());
            });
        }

        if (automatedTaskPatch.getArmReplayEndTs() != null) {
            armAutomatedTaskEntityConsumer.add(armAutomatedTaskEntity -> {
                armAutomatedTaskEntity.setArmReplayEndTs(automatedTaskPatch.getArmReplayEndTs());
                log.info("ARM replay end timestamp for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getArmReplayEndTs());
            });
        }

        if (automatedTaskPatch.getRpoCsvStartHour() != null) {
            armAutomatedTaskEntityConsumer.add(armAutomatedTaskEntity -> {
                armAutomatedTaskEntity.setRpoCsvStartHour(automatedTaskPatch.getRpoCsvStartHour());
                log.info("RPO CSV start hour for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getRpoCsvStartHour());
            });
        }

        if (automatedTaskPatch.getRpoCsvEndHour() != null) {
            armAutomatedTaskEntityConsumer.add(armAutomatedTaskEntity -> {
                armAutomatedTaskEntity.setRpoCsvEndHour(automatedTaskPatch.getRpoCsvEndHour());
                log.info("RPO CSV end hour for {} updated to {}", automatedTask.getTaskName(), automatedTaskPatch.getRpoCsvEndHour());
            });
        }

        if (armAutomatedTaskEntityConsumer.size() > 0) {
            ArmAutomatedTaskEntity armAutomatedTaskEntity = automatedTask.getArmAutomatedTaskEntity();
            if (armAutomatedTaskEntity == null) {
                throw new DartsApiException(INCORRECT_AUTOMATED_TASK_TYPE,
                                            "Task " + automatedTask.getTaskName() + " is not an arm automated task as such can not update arm related fields");
            }
            armAutomatedTaskEntityConsumer.forEach(consumer -> consumer.accept(armAutomatedTaskEntity));
            automatedTask.setArmAutomatedTaskEntity(armAutomatedTaskEntity);
            armAutomatedTaskRepository.save(armAutomatedTaskEntity);
        }

        var updatedTask = automatedTaskRepository.save(automatedTask);

        return mapper.mapEntityToDetailedAutomatedTask(updatedTask);
    }

    private boolean shouldIncludeAutomatedTask(AutomatedTaskEntity automatedTaskEntity) {
        AutomatedTaskName automatedTaskName = AutomatedTaskName.valueOfTaskName(automatedTaskEntity.getTaskName());
        if (automatedTaskName == null || automatedTaskName.getConditionalOnSpEL() == null) {
            return true;
        }
        String embeddedValue = Objects.requireNonNull(configurableBeanFactory.resolveEmbeddedValue(automatedTaskName.getConditionalOnSpEL()));
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
