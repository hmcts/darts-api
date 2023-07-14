package uk.gov.hmcts.darts.common.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

public interface AutomatedTaskService {
    public void loadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    public AutomatedTaskEntity getAutomatedTask(String taskName);

    public boolean cancelTaskTrigger(String taskName);

    public void loadAutomatedTaskOne(TaskScheduler taskScheduler);

}
