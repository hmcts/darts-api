package uk.gov.hmcts.darts.common.service;

import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

public interface AutomatedTaskService {
    void loadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    AutomatedTaskEntity getAutomatedTaskEntityByTaskName(String taskName);

    List<AutomatedTaskEntity> getAutomatedTaskEntitiesByTaskName(String taskName);

    boolean cancelAutomatedTask(String taskName);

    void loadAutomatedTaskOne(TaskScheduler taskScheduler);

    void updateAutomatedTaskCronExpression(String taskName, String cronExpression);

}
