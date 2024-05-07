package uk.gov.hmcts.darts.task.service;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.List;
import java.util.Optional;

public interface AutomatedTaskService {

    void configureAndLoadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    Optional<AutomatedTaskEntity> getAutomatedTaskEntityByTaskName(String taskName);

    boolean cancelAutomatedTask(String taskName, boolean mayInterruptIfRunning);

    boolean cancelAutomatedTaskAndUpdateCronExpression(String taskName, boolean mayInterruptIfRunning, String cronExpression);

    boolean updateAutomatedTaskCronExpression(String taskName, String cronExpression);

    void reloadTaskByName(String taskName);

    AutomatedTaskStatus getAutomatedTaskStatus(String taskName);

    List<AbstractLockableAutomatedTask> getAutomatedTasks();
}
