package uk.gov.hmcts.darts.task.service;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.util.Optional;

public interface AutomatedTaskService {

    void configureAndLoadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    Optional<AutomatedTaskEntity> getAutomatedTaskEntityByTaskName(String taskName);

    boolean cancelAutomatedTask(String taskName, boolean mayInterruptIfRunning);

    boolean cancelAutomatedTaskAndUpdateCronExpression(String taskName, boolean mayInterruptIfRunning, String cronExpression);

    boolean updateAutomatedTaskCronExpression(String taskName, String cronExpression);

    Class<? extends AutomatedTask> reloadTaskByName(String taskName);

    AutomatedTaskStatus getAutomatedTaskStatus(String taskName);

}