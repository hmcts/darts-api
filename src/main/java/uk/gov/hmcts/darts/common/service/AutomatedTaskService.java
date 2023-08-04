package uk.gov.hmcts.darts.common.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import java.util.List;

public interface AutomatedTaskService {

    void loadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    AutomatedTaskEntity getAutomatedTaskEntityByTaskName(String taskName);

    List<AutomatedTaskEntity> getAutomatedTaskEntitiesByTaskName(String taskName);

    boolean cancelAutomatedTask(String taskName);

    void loadAutomatedTaskOne(TaskScheduler taskScheduler);

    void updateAutomatedTaskCronExpression(String taskName, String cronExpression);

}
