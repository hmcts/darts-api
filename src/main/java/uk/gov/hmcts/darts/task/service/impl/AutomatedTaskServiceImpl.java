package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.model.TriggerAndAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutoloadingAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError.INVALID_CRON_EXPRESSION;

/**
 * Refer to <a href="https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-cron-expression">...</a>
 * for details of spring cron expressions.
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SuppressWarnings({"PMD.TooManyMethods", "PMD.TooFewBranchesForASwitchStatement", "PMD.SingularField", "PMD.CyclomaticComplexity",
    "PMD.CouplingBetweenObjects"})
public class AutomatedTaskServiceImpl implements AutomatedTaskService {

    private final AutomatedTaskRepository automatedTaskRepository;

    private final ScheduledTaskHolder taskHolder;

    private final TaskScheduler taskScheduler;

    private final Map<String, Trigger> taskTriggers = new ConcurrentHashMap<>();
    private final UserIdentity userIdentity;
    private final List<AutoloadingAutomatedTask> autoloadingAutomatedTasks;

    @Value("${darts.automated-tasks-pod:true}")
    private final boolean allowAutomatedTasks;

    @Override
    public void configureAndLoadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!allowAutomatedTasks) {
            log.info("Automated tasks are disabled");
            return;
        }
        log.info("Automated tasks are loading");
        autoloadingAutomatedTasks.forEach(autoloadingAutomatedTask -> {
            AbstractLockableAutomatedTask task = autoloadingAutomatedTask.getAbstractLockableAutomatedTask();
            log.info("Loading task: {}", task.getTaskName());
            task.setLastCronExpression(getAutomatedTaskCronExpression(task));
            Trigger trigger = createAutomatedTaskTrigger(task);
            taskRegistrar.addTriggerTask(task, trigger);
        });
    }

    @Override
    public Optional<AutomatedTaskEntity> getAutomatedTaskEntityByTaskName(String taskName) {
        return automatedTaskRepository.findByTaskName(taskName);
    }

    public String getAutomatedTaskCronExpression(AutomatedTask automatedTask) {
        String cronExpression = getAutomatedTaskCronExpression(automatedTask.getTaskName());
        if (cronExpression == null) {
            log.error("Unable to find cron expression for task: {}", automatedTask.getTaskName());
            throw new DartsApiException(INVALID_CRON_EXPRESSION);
        }
        return cronExpression;
    }

    public String getAutomatedTaskCronExpression(String taskName) {
        String cronExpression;
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskEntityByTaskName(taskName);
        if (automatedTaskEntity.isPresent()) {
            cronExpression = automatedTaskEntity.get().getCronExpression();
            log.debug("Task: {} cron expression: {}", taskName, cronExpression);
        } else {
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        return cronExpression;
    }

    /**
     * Updates the automated task cron expression or throws DartsApiException.
     * Calling this without the automated task being cancelled first, and then reloaded means the cron expression will only get picked up
     * after the next execution once the automated task has run and then calculates the next execution time. Cancelling
     * the automated task first means it will use the given cronExpression if the cron expression is valid.
     *
     * @param taskName       name of automated task
     * @param cronExpression cron expression
     * @return true if the automated task is successfully updated
     */
    @Override
    public boolean updateAutomatedTaskCronExpression(String taskName, String cronExpression) {
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskEntityByTaskName(taskName);
        if (automatedTaskEntity.isPresent()) {
            AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
            if (CronExpression.isValidExpression(cronExpression)) {
                automatedTask.setCronExpression(cronExpression);
                automatedTask.setLastModifiedBy(userIdentity.getUserAccount());
                automatedTaskRepository.saveAndFlush(automatedTask);
                log.debug("Updated the task {} with cron expression {}", taskName, cronExpression);
            } else {
                log.error("Unable to update the task {} with cron expression {}", taskName, cronExpression);
                throw new DartsApiException(INVALID_CRON_EXPRESSION);
            }
        } else {
            log.error("Failed to update the task {} with cron expression {}", taskName, cronExpression);
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        return true;
    }

    @Override
    public void reloadTaskByName(String taskName) {
        AutomatedTaskName automatedTaskName = AutomatedTaskName.valueOfTaskName(taskName);
        if (isNull(automatedTaskName)) {
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
        boolean found = false;
        for (AutoloadingAutomatedTask autoloadingAutomatedTask : autoloadingAutomatedTasks) {
            if (automatedTaskName.equals(autoloadingAutomatedTask.getAutomatedTaskName())) {
                found = true;
                TriggerAndAutomatedTask triggerAndAutomatedTask = getTriggerAndAutomatedTask(autoloadingAutomatedTask.getTaskName());
                if (triggerAndAutomatedTask == null) {
                    var abstractLockableAutomatedTask = autoloadingAutomatedTask.getAbstractLockableAutomatedTask();
                    Trigger trigger = createAutomatedTaskTrigger(abstractLockableAutomatedTask);
                    taskScheduler.schedule(abstractLockableAutomatedTask, trigger);
                } else {
                    taskScheduler.schedule(triggerAndAutomatedTask.getAutomatedTask(), triggerAndAutomatedTask.getTrigger());
                }
                break;
            }
        }
        if (!found) {
            throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
        }
    }

    @Override
    public boolean cancelAutomatedTaskAndUpdateCronExpression(String taskName, boolean mayInterruptIfRunning, String cronExpression) {
        boolean cancelled = cancelAutomatedTask(taskName, mayInterruptIfRunning);
        boolean updatedCronExpression = false;
        if (cancelled) {
            updatedCronExpression = updateAutomatedTaskCronExpression(taskName, cronExpression);
            if (updatedCronExpression) {
                reloadTaskByName(taskName);
            } else {
                throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
            }
        }
        return cancelled && updatedCronExpression;
    }

    @Override
    public boolean cancelAutomatedTask(String taskName, boolean mayInterruptIfRunning) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask && cancelTriggerTask(
                taskName,
                scheduledTask,
                triggerTask,
                mayInterruptIfRunning
            )) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AutomatedTaskStatus getAutomatedTaskStatus(String taskName) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask
                && triggerTask.getRunnable() instanceof AutomatedTask automatedTask
                && automatedTask.getTaskName().equals(taskName)) {
                return automatedTask.getAutomatedTaskStatus();
            }
        }
        throw new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK);
    }

    @Override
    public ArmAutomatedTaskEntity getArmAutomatedTaskEntity(AutomatedTaskName automatedTaskName) {
        return automatedTaskRepository.findByTaskName(automatedTaskName.getTaskName())
            .map(automatedTaskEntity -> automatedTaskEntity.getArmAutomatedTaskEntity())
            .orElseThrow(() -> new DartsApiException(FAILED_TO_FIND_AUTOMATED_TASK));
    }

    private TriggerAndAutomatedTask getTriggerAndAutomatedTask(String taskName) {
        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            Task task = scheduledTask.getTask();
            if (task instanceof TriggerTask triggerTask
                && triggerTask.getRunnable() instanceof AbstractLockableAutomatedTask automatedTask
                && automatedTask.getTaskName().equals(taskName)) {
                return TriggerAndAutomatedTask.builder()
                    .automatedTask(automatedTask)
                    .trigger(triggerTask.getTrigger())
                    .build();
            }
        }
        return null;
    }

    private boolean cancelTriggerTask(String taskName, ScheduledTask scheduledTask, TriggerTask triggerTask, boolean mayInterruptIfRunning) {
        if (triggerTask.getRunnable() instanceof AutomatedTask automatedTask && automatedTask.getTaskName().equals(taskName)) {
            log.info("About to cancel task: " + taskName);
            scheduledTask.cancel(mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    private Trigger createAutomatedTaskTrigger(AutomatedTask automatedTask) {
        Trigger trigger = createCronTrigger(automatedTask);
        taskTriggers.put(automatedTask.getTaskName(), trigger);
        return trigger;
    }

    private Trigger createCronTrigger(AutomatedTask automatedTask) {
        return triggerContext -> {
            String cronExpression = getAutomatedTaskCronExpression(automatedTask);
            log.debug(
                "Creating trigger for task: {} with cron expression: {} ",
                automatedTask.getTaskName(),
                cronExpression
            );
            automatedTask.setLastCronExpression(cronExpression);
            CronTrigger crontrigger = new CronTrigger(cronExpression);
            return crontrigger.nextExecution(triggerContext);
        };
    }
}